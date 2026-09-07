package com.nercare.cogcare.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.ai.AiIntentEngine
import com.nercare.cogcare.ai.AiIntentResult
import com.nercare.cogcare.ai.NluIntent
import com.nercare.cogcare.ai.StateManager
import com.nercare.cogcare.ai.TherapeuticResponseGenerator
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.data.repository.PatientRepository
import com.nercare.cogcare.data.repository.ReminderRepository
import com.nercare.cogcare.reminder.ReminderScheduler
import com.nercare.cogcare.domain.model.*
import com.nercare.cogcare.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nercare.cogcare.ai.litert.LiteRtAiEngine
import java.util.Locale

// ── Voice Session State Machine ───────────────────────────────────────────────
sealed class VoiceSessionState {
    /** App speaks the question aloud (TTS playing) */
    object Speaking : VoiceSessionState()
    /** Microphone is active — waiting for patient to respond */
    data class Listening(val isPaused: Boolean = false) : VoiceSessionState()
    object Typing : VoiceSessionState()
    /** System is processing / transcribing what patient said */
    object Processing : VoiceSessionState()
    /** AI has a response / memory confirmation to show */
    data class Responding(val message: String) : VoiceSessionState()
    /** Session complete — patient confirmed or action executed */
    data class Completed(val resultMessage: String) : VoiceSessionState()
    /** No pending questions in the queue today */
    object NoPendingQuestions : VoiceSessionState()
    /** Direct navigation action triggered by voice (e.g. to a game) */
    data class Navigate(val route: String) : VoiceSessionState()
    /** Error state */
    data class Error(val message: String) : VoiceSessionState()
}

data class VoiceUiState(
    val patientId: String = "",
    val sessionState: VoiceSessionState = VoiceSessionState.Speaking,
    val currentQuestion: CompanionQuestion? = null,
    val activePrompt: String = "",
    val spokenTranscript: String = "",
    val draftText: String = "",
    val showAmendField: Boolean = false,
    val amendText: String = "",
    val todayGreeting: String = "",
    val isLoadingNext: Boolean = false,
    val lastIntent: NluIntent? = null,
    val isCloudEngineActive: Boolean = false,
    val isLifeStoryMode: Boolean = false,
    val isOnDeviceModelInstalled: Boolean = false,
    val modelInstallMessage: String? = null
)

@HiltViewModel
class CompanionVoiceViewModel @Inject constructor(
    private val lifeStoryRepository: LifeStoryRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val patientRepository: PatientRepository,
    private val aiEngine: AiIntentEngine,
    private val stateManager: StateManager,
    private val networkMonitor: NetworkMonitor,
    private val therapeuticResponseGenerator: TherapeuticResponseGenerator,
    private val liteRtAiEngine: LiteRtAiEngine,
    @ApplicationContext private val context: Context
) : ViewModel(), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var chatHistory = mutableListOf<Pair<String, String>>()
    private var isLifeStoryMode = false
    private var initializedSessionKey: String? = null
    private var wellnessPromptShown = false
    private val sessionAskedPrompts = mutableSetOf<String>()

    private var patientName: String = "Friend"
    private var familyAnchorSummary: String = "Your family is always with you."
    private var currentPatient: Patient? = null
    private var currentFamilyMembers: List<FamilyMember> = emptyList()

    init {
        textToSpeech = TextToSpeech(context, this)

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isCloudEngineActive = online) }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback if IN is not supported (though usually is)
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setSpeechRate(0.85f)
            isTtsReady = true
            val prompt = _uiState.value.activePrompt
            if (_uiState.value.sessionState is VoiceSessionState.Speaking && prompt.isNotBlank()) {
                speakQuestion(prompt)
            }
        } else {
            moveToListening()
        }
    }

    override fun onCleared() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onCleared()
    }

    private fun speakOut(text: String, onDone: (() -> Unit)? = null) {
        if (!isTtsReady) {
            onDone?.invoke()
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) {
                if (id == utteranceId) onDone?.invoke()
            }
            @Deprecated("Deprecated in Android")
            override fun onError(id: String?) {
                if (id == utteranceId) onDone?.invoke()
            }
        })
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }


    fun init(patientId: String, mode: String = "default") {
        val sessionKey = "$patientId:$mode"
        if (initializedSessionKey == sessionKey) return
        initializedSessionKey = sessionKey
        isLifeStoryMode = (mode == "lifestory")
        wellnessPromptShown = false
        sessionAskedPrompts.clear()
        _uiState.update {
            it.copy(
                patientId = patientId,
                todayGreeting = buildGreeting(),
                isLifeStoryMode = isLifeStoryMode,
                isOnDeviceModelInstalled = liteRtAiEngine.isModelInstalled
            )
        }
        loadPatientData(patientId)
        if (isLifeStoryMode) {
            loadNextQuestion(patientId)
        } else {
            presentAssistantPrompt("How can I help you today?")
        }
    }

    private fun loadPatientData(patientId: String) {
        viewModelScope.launch {
            val patient = patientRepository.getPatientById(patientId)
            currentPatient = patient
            if (patient != null) {
                patientName = patient.name
                val familyList: List<FamilyMember> = lifeStoryRepository.observeAllFamilyMembers(patientId).firstOrNull() ?: emptyList()
                currentFamilyMembers = familyList
                if (familyList.isNotEmpty()) {
                    val names = familyList.take(2).joinToString(" and ") { "${it.fullName} (${it.relation.displayLabel})" }
                    familyAnchorSummary = "$names love you very much."
                }
            }
        }
    }

    // ── Core: Load the next pending question for this patient ─────────────────
    private fun loadNextQuestion(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNext = true) }
            
            // 1. Ensure patient and family details are loaded for rich personalization
            if (currentPatient == null) {
                currentPatient = patientRepository.getPatientById(patientId)
                if (currentPatient != null) {
                    patientName = currentPatient!!.name
                }
            }
            if (currentFamilyMembers.isEmpty()) {
                currentFamilyMembers = lifeStoryRepository.observeAllFamilyMembers(patientId).firstOrNull() ?: emptyList()
            }

            // 2. Fetch all existing memories to give context
            val existingMemories = lifeStoryRepository.observeAllMemoryNodes(patientId).firstOrNull() ?: emptyList()
            
            // 3. Try to generate personal starter or select from clinical catalog
            var candidatePrompt = runCatching {
                aiEngine.generatePersonalizedStarterQuestion(
                    knownMemories = existingMemories,
                    patient = currentPatient,
                    familyMembers = currentFamilyMembers,
                    sessionAskedPrompts = sessionAskedPrompts
                )
            }.getOrNull()?.trim()

            // If empty, null, already asked in this session, or duplicate in memories:
            if (candidatePrompt.isNullOrBlank() ||
                sessionAskedPrompts.any { it.equals(candidatePrompt, ignoreCase = true) } ||
                com.nercare.cogcare.domain.model.ReminiscenceQuestionCatalog.isDuplicate(candidatePrompt, existingMemories)
            ) {
                val catalogQuestion = com.nercare.cogcare.domain.model.ReminiscenceQuestionCatalog.getNextUnansweredQuestion(
                    knownMemories = existingMemories,
                    sessionAskedPrompts = sessionAskedPrompts
                )
                candidatePrompt = catalogQuestion.prompt
            }

            sessionAskedPrompts.add(candidatePrompt)
            
            val question = com.nercare.cogcare.domain.model.CompanionQuestion(
                patientId = patientId,
                domain = "REMINISCENCE",
                nodeKey = "reminiscence_${System.currentTimeMillis()}",
                questionPrompt = candidatePrompt
            )

            _uiState.update {
                it.copy(
                    currentQuestion = question,
                    activePrompt = question.questionPrompt,
                    sessionState = VoiceSessionState.Speaking,
                    isLoadingNext = false
                )
            }
            speakQuestion(question.questionPrompt)
        }
    }

    private fun speakQuestion(question: String) {
        if (!isTtsReady) return
        speakOut(question) { moveToListening() }
    }

    private fun presentAssistantPrompt(prompt: String) {
        _uiState.update {
            it.copy(
                currentQuestion = null,
                activePrompt = prompt,
                sessionState = VoiceSessionState.Speaking
            )
        }
        speakQuestion(prompt)
    }

    private fun moveToListening() {
        _uiState.update { it.copy(sessionState = VoiceSessionState.Listening()) }
    }

    fun onMicTapped() {
        _uiState.update { it.copy(sessionState = VoiceSessionState.Listening()) }
    }

    fun onTypeInstead() {
        textToSpeech?.stop()
        _uiState.update { it.copy(sessionState = VoiceSessionState.Typing) }
    }

    fun onDraftTextChange(text: String) {
        _uiState.update { it.copy(draftText = text) }
    }

    fun onSubmitDraft() {
        val text = _uiState.value.draftText.trim()
        if (text.isBlank()) return
        _uiState.update { it.copy(draftText = "") }
        onTextSubmitted(text)
    }

    fun onSpeechRecognitionError() {
        _uiState.update { it.copy(sessionState = VoiceSessionState.Typing) }
    }

    fun onSpeechTimeout() {
        _uiState.update { it.copy(sessionState = VoiceSessionState.Listening(isPaused = true)) }
    }

    // ── Patient speech transcribed ───────────────────────────────────────────
    fun onSpeechResult(transcribedText: String) {
        val isAwaitingAnswer = isLifeStoryMode && _uiState.value.currentQuestion != null &&
                _uiState.value.sessionState !is VoiceSessionState.NoPendingQuestions

        _uiState.update {
            it.copy(
                spokenTranscript = transcribedText,
                sessionState = VoiceSessionState.Processing
            )
        }

        viewModelScope.launch {
            delay(600L)
            
            // Append user input to history
            chatHistory.add(Pair("Patient", transcribedText))
            
            val activePromptText = _uiState.value.currentQuestion?.questionPrompt ?: _uiState.value.activePrompt
            val aiResult = aiEngine.process(
                input = transcribedText,
                isAwaitingQuestionAnswer = isAwaitingAnswer,
                chatHistory = chatHistory,
                patient = currentPatient,
                activeQuestion = activePromptText
            )
            _uiState.update { it.copy(lastIntent = aiResult.intent) }
            handleAiResult(aiResult, transcribedText)
        }
    }

    fun onTextSubmitted(text: String) {
        onSpeechResult(text)
    }

    fun installOnDeviceModel(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(modelInstallMessage = "Installing on-device AI model…") }
            runCatching { liteRtAiEngine.installModel(uri) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isOnDeviceModelInstalled = true,
                            modelInstallMessage = "On-device AI model installed"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(modelInstallMessage = error.message ?: "Model installation failed")
                    }
                }
        }
    }

    private suspend fun handleAiResult(aiResult: AiIntentResult, text: String) {
        val currentState = stateManager.peekCurrentState()

        // Questionnaire behavior belongs only to Tell Us About Yourself. Save
        // every answer before moving on so a failed extraction cannot cause the
        // same starter question to repeat forever.
        if (isLifeStoryMode && _uiState.value.currentQuestion != null) {
            if (aiResult.intent == NluIntent.SKIP_QUESTION) {
                onSkipQuestion()
                return
            }
            val question = _uiState.value.currentQuestion!!
            sessionAskedPrompts.add(question.questionPrompt)
            
            // 1. EXTRACT & SAVE MEMORY TO ROOM DB SIDE-BY-SIDE
            val savedValue = aiResult.extractedMemory?.ifBlank { null } ?: text
            lifeStoryRepository.saveMemoryNode(
                LifeMemoryNode(
                    patientId = question.patientId,
                    domain = aiResult.memoryDomain ?: question.domain,
                    nodeKey = "life_story_${System.currentTimeMillis()}",
                    questionPrompt = question.questionPrompt,
                    value = savedValue,
                    source = MemorySource.PATIENT_VOICE,
                    verificationStatus = VerificationStatus.VERIFIED_BY_PATIENT,
                    dateVerified = System.currentTimeMillis()
                )
            )

            // 2. ASK RELATED QUESTION & CONTINUE THE CONVERSATION
            val followUp = aiResult.generatedFollowUpQuestion
                ?.takeIf { it.isNotBlank() && !it.equals(question.questionPrompt, ignoreCase = true) && !sessionAskedPrompts.contains(it) }
            if (followUp != null) {
                sessionAskedPrompts.add(followUp)
                val nextQuestion = CompanionQuestion(
                    patientId = question.patientId,
                    domain = aiResult.memoryDomain ?: "DYNAMIC",
                    nodeKey = "life_story_${System.currentTimeMillis()}",
                    questionPrompt = followUp
                )
                chatHistory.add(Pair("Saathi", followUp))
                
                val reaction = aiResult.assistantResponse?.trim()
                    ?: "Thank you so much for sharing that with me."
                val spokenSpeech = "$reaction $followUp"

                _uiState.update {
                    it.copy(
                        currentQuestion = nextQuestion,
                        activePrompt = followUp,
                        spokenTranscript = "",
                        sessionState = VoiceSessionState.Speaking
                    )
                }
                speakOut(spokenSpeech) {
                    moveToListening()
                }
            } else {
                val message = aiResult.assistantResponse
                    ?: "Thank you so much for sharing that with me! I will remember it."
                _uiState.update { it.copy(sessionState = VoiceSessionState.Responding(message)) }
                speakOut(message) { loadNextQuestion(question.patientId) }
            }
            return
        }

        when (aiResult.intent) {
            NluIntent.FEELING_ANXIOUS -> {
                val response = therapeuticResponseGenerator.generateResponse(aiResult, currentState, _uiState.value.patientId, patientName)
                speakOut(response)
                _uiState.update {
                    it.copy(sessionState = VoiceSessionState.Responding(response))
                }
            }

            NluIntent.SET_REMINDER -> {
                val reminder = aiResult.parsedReminder
                if (reminder != null) {
                    val newReminder = Reminder(
                        id = UUID.randomUUID().toString(),
                        patientId = _uiState.value.patientId,
                        type = if (reminder.title.lowercase().contains("medicine") || reminder.title.lowercase().contains("pill"))
                            ReminderType.MEDICINE else ReminderType.ACTIVITY,
                        title = reminder.title,
                        description = "Created via Voice Companion",
                        hour = reminder.hour,
                        minute = reminder.minute,
                        repeatDays = listOf(1, 2, 3, 4, 5, 6, 7),
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                    val savedReminder = reminderRepository.saveReminder(newReminder)
                    reminderScheduler.scheduleReminder(savedReminder, patientName)
                    val timeStr = formatTime(reminder.hour, reminder.minute)
                    val msg = "I have set a reminder for \"${reminder.title}\" at $timeStr! ⏰"
                    speakOut(msg)
                    _uiState.update {
                        it.copy(
                            sessionState = VoiceSessionState.Completed(msg)
                        )
                    }
                } else {
                    val msg = "What time would you like me to set the reminder for?"
                    speakOut(msg)
                    _uiState.update {
                        it.copy(
                            sessionState = VoiceSessionState.Responding(msg)
                        )
                    }
                }
            }

            NluIntent.CHECK_REMINDERS -> {
                val activeList = reminderRepository.getAllActiveReminders()
                    .filter { it.patientId == _uiState.value.patientId }
                if (activeList.isEmpty()) {
                    val msg = "You have no pending reminders right now. Everything is peaceful! 🌿"
                    speakOut(msg)
                    _uiState.update {
                        it.copy(sessionState = VoiceSessionState.Responding(msg))
                    }
                } else {
                    val next = activeList.first()
                    val timeStr = formatTime(next.hour, next.minute)
                    val msg = "Your next reminder is \"${next.title}\" at $timeStr."
                    speakOut(msg)
                    _uiState.update {
                        it.copy(
                            sessionState = VoiceSessionState.Responding(msg)
                        )
                    }
                }
            }

            NluIntent.PLAY_GAME -> {
                val msg = "Let's play! Opening your cognitive games now. 🎮"
                speakOut(msg)
                _uiState.update {
                    it.copy(sessionState = VoiceSessionState.Completed(msg))
                }
                val targetRoute = aiResult.targetGameRoute ?: "games"
                _navigationEvent.emit(targetRoute)
            }

            NluIntent.CONFIRM_YES -> onPatientConfirmed()

            NluIntent.CONFIRM_NO -> onPatientWantsToAmend()

            NluIntent.EMERGENCY_SOS -> {
                val response = therapeuticResponseGenerator.generateResponse(aiResult, currentState, _uiState.value.patientId, patientName)
                speakOut(response)
                _uiState.update {
                    it.copy(sessionState = VoiceSessionState.Responding(response))
                }
            }

            NluIntent.SKIP_QUESTION -> onSkipQuestion()

            NluIntent.REMEMBER_THIS -> {
                val domain = aiResult.memoryDomain ?: "EMOTIONAL_ANCHORS"
                val key = aiResult.memoryKey ?: "memory_${System.currentTimeMillis()}"
                val node = LifeMemoryNode(
                    patientId = _uiState.value.patientId,
                    domain = domain,
                    nodeKey = key,
                    questionPrompt = "Patient voice memory",
                    value = text,
                    source = MemorySource.PATIENT_VOICE,
                    verificationStatus = VerificationStatus.VERIFIED_BY_PATIENT,
                    dateVerified = System.currentTimeMillis()
                )
                lifeStoryRepository.saveMemoryNode(node)
                val msg = "I have saved that in your Life Story! 📖✨"
                speakOut(msg)
                _uiState.update {
                    it.copy(
                        sessionState = VoiceSessionState.Completed(msg)
                    )
                }
            }

            NluIntent.TELL_ME_ABOUT -> {
                val response = aiResult.assistantResponse
                    ?: therapeuticResponseGenerator.generateResponse(aiResult, currentState, _uiState.value.patientId, patientName)
                speakOut(response)
                _uiState.update {
                    it.copy(
                        sessionState = VoiceSessionState.Responding(response)
                    )
                }
            }

            NluIntent.COMPANION_CHITCHAT -> {
                val reply = aiResult.assistantResponse?.ifBlank { null }
                    ?: "Namaste! I am right here with you. How can I help you today?"
                _uiState.update {
                    it.copy(
                        spokenTranscript = text,
                        sessionState = VoiceSessionState.Responding(reply)
                    )
                }
                speakOut(reply)
            }

            NluIntent.ANSWER_QUESTION -> {
                // If in life story mode, advance to next question
                if (isLifeStoryMode) {
                    val q = _uiState.value.currentQuestion
                    if (q != null) {
                        val node = LifeMemoryNode(
                            patientId = _uiState.value.patientId,
                            domain = q.domain,
                            nodeKey = q.nodeKey,
                            questionPrompt = q.questionPrompt,
                            value = text,
                            source = MemorySource.PATIENT_VOICE,
                            verificationStatus = VerificationStatus.VERIFIED_BY_PATIENT,
                            dateVerified = System.currentTimeMillis()
                        )
                        lifeStoryRepository.saveMemoryNode(node)
                    }

                    val msg = aiResult.assistantResponse?.ifBlank { null }
                        ?: "Thank you for sharing that with me! 🌸"
                    _uiState.update {
                        it.copy(
                            spokenTranscript = text,
                            sessionState = VoiceSessionState.Responding(msg)
                        )
                    }
                    speakOut(msg) { loadNextQuestion(_uiState.value.patientId) }
                } else {
                    val msg = aiResult.assistantResponse?.ifBlank { null }
                        ?: "I understand! Tell me more."
                    _uiState.update {
                        it.copy(
                            spokenTranscript = text,
                            sessionState = VoiceSessionState.Responding(msg)
                        )
                    }
                    speakOut(msg)
                }
            }

            NluIntent.CALM_ACKNOWLEDGEMENT -> {
                val msg = aiResult.reassuranceMessage ?: "I am glad you are feeling better."
                speakOut(msg)
                _uiState.update {
                    it.copy(
                        spokenTranscript = text,
                        sessionState = VoiceSessionState.Responding(msg)
                    )
                }
            }

            NluIntent.UNKNOWN -> {
                val q = _uiState.value.currentQuestion
                if (isLifeStoryMode && q != null && _uiState.value.sessionState !is VoiceSessionState.NoPendingQuestions) {
                    val node = LifeMemoryNode(
                        patientId = _uiState.value.patientId,
                        domain = q.domain,
                        nodeKey = q.nodeKey,
                        questionPrompt = q.questionPrompt,
                        value = text,
                        source = MemorySource.PATIENT_VOICE,
                        verificationStatus = VerificationStatus.VERIFIED_BY_PATIENT,
                        dateVerified = System.currentTimeMillis()
                    )
                    lifeStoryRepository.saveMemoryNode(node)
                    
                    val msg = aiResult.assistantResponse?.ifBlank { null }
                        ?: "Got it! Thank you for sharing. 🌿"
                    _uiState.update {
                        it.copy(
                            spokenTranscript = text,
                            sessionState = VoiceSessionState.Responding(msg)
                        )
                    }
                    speakOut(msg) { loadNextQuestion(_uiState.value.patientId) }
                } else {
                    val msg = aiResult.assistantResponse?.ifBlank { null }
                        ?: "I am right here with you. What would you like to talk about?"
                    _uiState.update {
                        it.copy(
                            spokenTranscript = text,
                            sessionState = VoiceSessionState.Responding(msg)
                        )
                    }
                    speakOut(msg)
                }
            }
        }
    }

    // ── Patient confirmed the memory ("Yes, that's right!") ───────────────────
    fun onPatientConfirmed() {
        val q = _uiState.value.currentQuestion
        viewModelScope.launch {
            val answer = _uiState.value.spokenTranscript.trim()
            if (q == null) {
                if (answer.isNotBlank()) {
                    lifeStoryRepository.saveMemoryNode(
                        LifeMemoryNode(
                            patientId = _uiState.value.patientId,
                            domain = "EMOTIONAL_ANCHORS",
                            nodeKey = "shared_memory_${System.currentTimeMillis()}",
                            questionPrompt = "Memory shared with Saathi",
                            value = answer,
                            source = MemorySource.PATIENT_TYPED,
                            verificationStatus = VerificationStatus.VERIFIED_BY_PATIENT,
                            dateVerified = System.currentTimeMillis()
                        )
                    )
                }
                _uiState.update { it.copy(sessionState = VoiceSessionState.Completed("I have remembered that!")) }
                return@launch
            }
            val caregiverValue = answer.ifBlank { q.caregiverHypothesis.orEmpty() }
            if (caregiverValue.isNotBlank()) {
                val node = LifeMemoryNode(
                    patientId = q.patientId,
                    domain = q.domain,
                    nodeKey = q.nodeKey,
                    questionPrompt = q.questionPrompt,
                    value = caregiverValue,
                    source = MemorySource.PATIENT_VOICE,
                    verificationStatus = VerificationStatus.VERIFIED_BY_PATIENT,
                    dateVerified = System.currentTimeMillis()
                )
                lifeStoryRepository.saveMemoryNode(node)
            }
            _uiState.update {
                it.copy(
                    sessionState = VoiceSessionState.Completed(
                        "I have remembered that! 🌿"
                    )
                )
            }
        }
    }

    // ── Patient wants to amend/correct something ───────────────────────────────
    fun onPatientWantsToAmend() {
        textToSpeech?.stop()
        _uiState.update { state ->
            val responding = state.sessionState as? VoiceSessionState.Responding
            state.copy(
                showAmendField = true,
                amendText = state.spokenTranscript,
                sessionState = responding ?: VoiceSessionState.Responding("Please type the correct answer.")
            )
        }
    }

    fun onAmendTextChange(text: String) {
        _uiState.update { it.copy(amendText = text) }
    }

    fun onSubmitAmendment() {
        val q = _uiState.value.currentQuestion ?: return
        val amendment = _uiState.value.amendText.trim()
        if (amendment.isBlank()) return

        viewModelScope.launch {
            val node = LifeMemoryNode(
                patientId = q.patientId,
                domain = q.domain,
                nodeKey = q.nodeKey,
                questionPrompt = q.questionPrompt,
                value = amendment,
                source = MemorySource.PATIENT_VOICE,
                verificationStatus = VerificationStatus.DISPUTED_AMENDED,
                dateVerified = System.currentTimeMillis()
            )
            lifeStoryRepository.saveMemoryNode(node)
            _uiState.update {
                it.copy(
                    showAmendField = false,
                    sessionState = VoiceSessionState.Completed(
                        "Thank you for telling me. I will remember \"$amendment\" — your truth matters most! 💚"
                    )
                )
            }
        }
    }

    // ── Patient wants to skip question ─────────────────────────────────────────
    fun onSkipQuestion() {
        val q = _uiState.value.currentQuestion
        if (q != null) {
            sessionAskedPrompts.add(q.questionPrompt)
        }
        viewModelScope.launch {
            val skipMessage = "No problem at all! Let's talk about something else. 🌸"
            _uiState.update {
                it.copy(
                    sessionState = VoiceSessionState.Responding(skipMessage)
                )
            }
            speakOut(skipMessage) {
                loadNextQuestion(_uiState.value.patientId)
            }
        }
    }

    fun onPatientSharesMemory(spokenMemory: String) {
        onSpeechResult(spokenMemory)
    }

    fun onStartNextQuestion() {
        _uiState.update { it.copy(spokenTranscript = "", draftText = "", amendText = "", showAmendField = false, currentQuestion = null) }
        if (isLifeStoryMode) {
            loadNextQuestion(_uiState.value.patientId)
        } else {
            val prompt = if (!wellnessPromptShown) {
                wellnessPromptShown = true
                "How are you feeling today?"
            } else {
                "What else can I help you with?"
            }
            presentAssistantPrompt(prompt)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        return "${h}:${minute.toString().padStart(2, '0')} $amPm"
    }

    private fun buildGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning! ☀️"
            in 12..16 -> "Good afternoon! 🌤️"
            in 17..20 -> "Good evening! 🌅"
            else -> "Good night! 🌙"
        }
    }
}
