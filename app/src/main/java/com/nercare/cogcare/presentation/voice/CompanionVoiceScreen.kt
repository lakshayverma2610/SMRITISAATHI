package com.nercare.cogcare.presentation.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.domain.model.ReminiscenceQuestionCatalog
import com.nercare.cogcare.presentation.theme.*

@Composable
fun CompanionVoiceScreen(
    patientId: String,
    mode: String = "default",
    onNavigateToGames: () -> Unit,
    onBack: () -> Unit,
    viewModel: CompanionVoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var recognitionRequest by remember { mutableIntStateOf(0) }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recognitionRequest++ else viewModel.onSpeechRecognitionError()
    }

    fun requestListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            viewModel.onSpeechRecognitionError()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            recognitionRequest++
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_CLIENT) {
                    // Ignore client error triggered by cancel()
                    return
                }
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    viewModel.onSpeechTimeout()
                } else {
                    android.widget.Toast.makeText(context, "Mic Error: $error", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.onSpeechRecognitionError()
                }
            }
            override fun onResults(results: Bundle?) {
                val transcript = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (transcript.isNullOrBlank()) viewModel.onSpeechRecognitionError()
                else viewModel.onSpeechResult(transcript)
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        speechRecognizer = recognizer
        onDispose {
            recognizer.cancel()
            recognizer.destroy()
            speechRecognizer = null
        }
    }

    LaunchedEffect(uiState.sessionState) {
        if (uiState.sessionState is VoiceSessionState.Listening) requestListening()
    }

    LaunchedEffect(recognitionRequest) {
        if (recognitionRequest > 0) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            }
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
        }
    }

    LaunchedEffect(patientId, mode) { viewModel.init(patientId, mode) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            if (route.contains("game", ignoreCase = true)) {
                onNavigateToGames()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = PrimaryGreen, modifier = Modifier.size(28.dp))
        }

        // AI Status Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isOnDeviceModelInstalled) {
                Text("On-device AI ready", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
            } else if (uiState.isCloudEngineActive) {
                Text("Cloud AI ready", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
            } else {
                Text("AI model unavailable", style = MaterialTheme.typography.labelSmall, color = TextSecondaryMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Greeting
            Text(
                text = uiState.todayGreeting,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondaryMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Central content changes based on state
            AnimatedContent(
                targetState = uiState.sessionState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "voice_state_anim",
                modifier = Modifier.weight(1f)
            ) { sessionState ->
                when (sessionState) {
                    is VoiceSessionState.Speaking -> SpeakingContent(uiState.activePrompt)
                    is VoiceSessionState.Listening -> ListeningContent(
                        questionText = uiState.activePrompt,
                        onMicTap = { requestListening() },
                        isPaused = sessionState.isPaused
                    )
                    is VoiceSessionState.Typing -> TypingContent(
                        questionText = uiState.activePrompt,
                        value = uiState.draftText,
                        onValueChange = viewModel::onDraftTextChange,
                        onSubmit = viewModel::onSubmitDraft,
                        onUseMicrophone = viewModel::onMicTapped
                    )
                    is VoiceSessionState.Processing -> ProcessingContent()
                    is VoiceSessionState.Responding -> RespondingContent(
                        message = sessionState.message,
                        currentQuestionHypothesis = uiState.currentQuestion?.caregiverHypothesis,
                        showAmendField = uiState.showAmendField,
                        amendText = uiState.amendText,
                        showConfirmationActions = uiState.isLifeStoryMode && uiState.currentQuestion?.caregiverHypothesis != null,
                        isLifeStoryMode = uiState.isLifeStoryMode,
                        onConfirm = { viewModel.onPatientConfirmed() },
                        onAmend = { viewModel.onPatientWantsToAmend() },
                        onAmendTextChange = { viewModel.onAmendTextChange(it) },
                        onSubmitAmendment = { viewModel.onSubmitAmendment() },
                        onSkip = { viewModel.onSkipQuestion() },
                        onContinue = { viewModel.onStartNextQuestion() },
                        onTalkAgain = { viewModel.onMicTapped() },
                        onTypeInstead = { viewModel.onTypeInstead() }
                    )
                    is VoiceSessionState.Completed -> CompletedContent(
                        message = sessionState.resultMessage,
                        onNext = { viewModel.onStartNextQuestion() },
                        onBack = onBack
                    )
                    is VoiceSessionState.NoPendingQuestions -> NoPendingContent(
                        onShareMemory = { viewModel.onPatientSharesMemory(it) },
                        onBack = onBack
                    )
                    is VoiceSessionState.Navigate -> {
                        LaunchedEffect(sessionState.route) { onNavigateToGames() }
                        ProcessingContent()
                    }
                    is VoiceSessionState.Error -> ErrorContent(
                        message = sessionState.message,
                        onBack = onBack
                    )
                }
            }

            // Bottom hint chips (always visible except when done)
            val showHints = uiState.sessionState is VoiceSessionState.Listening ||
                    uiState.sessionState is VoiceSessionState.Speaking

            AnimatedVisibility(visible = showHints) {
                VoiceHintChips(
                    onTypeInstead = { viewModel.onTypeInstead() },
                    onSkip = if (uiState.isLifeStoryMode) ({ viewModel.onSkipQuestion() }) else null
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SPEAKING: App is saying the question aloud (TTS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpeakingContent(questionText: String) {
    val categoryBadge = ReminiscenceQuestionCatalog.QUESTIONS.find {
        it.prompt.equals(questionText, ignoreCase = true)
    }?.let { "${it.categoryEmoji} ${it.categoryLabel}" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated waveform
        SoundwaveAnimation(isActive = true, color = SecondaryGreen)

        Spacer(Modifier.height(32.dp))

        if (categoryBadge != null) {
            Surface(
                color = PrimaryGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = categoryBadge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }

        Surface(
            color = SecondaryGreen,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = questionText,
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = PrimaryGreen,
                lineHeight = 36.sp
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Listening for your answer...", style = MaterialTheme.typography.bodyLarge, color = TextSecondaryMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LISTENING: Mic is active, pulsing animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ListeningContent(questionText: String, onMicTap: () -> Unit, isPaused: Boolean) {
    val categoryBadge = ReminiscenceQuestionCatalog.QUESTIONS.find {
        it.prompt.equals(questionText, ignoreCase = true)
    }?.let { "${it.categoryEmoji} ${it.categoryLabel}" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Pulsing mic
        PulsingMicButton(onTap = onMicTap, isPaused = isPaused)

        Spacer(Modifier.height(28.dp))
        
        if (isPaused) {
            Text("Tap the mic to speak", style = MaterialTheme.typography.titleLarge, color = PrimaryGreen, fontWeight = FontWeight.Bold)
        } else {
            Text("I am listening...", style = MaterialTheme.typography.titleLarge, color = PrimaryGreen, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(8.dp))
        Text("Speak clearly in your language", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryMuted)
        if (questionText.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            if (categoryBadge != null) {
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = categoryBadge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }
            Surface(
                color = SecondaryGreen.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = questionText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryGreen,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TypingContent(
    questionText: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseMicrophone: () -> Unit
) {
    val categoryBadge = ReminiscenceQuestionCatalog.QUESTIONS.find {
        it.prompt.equals(questionText, ignoreCase = true)
    }?.let { "${it.categoryEmoji} ${it.categoryLabel}" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (categoryBadge != null) {
            Surface(
                color = PrimaryGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = categoryBadge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }

        Text(
            text = questionText.ifBlank { "What would you like to tell Saathi?" },
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryGreen,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type your answer or command...") },
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = value.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Send to Saathi", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onUseMicrophone) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Try microphone again")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROCESSING: Spinner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProcessingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 6.dp, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Understanding what you said...", style = MaterialTheme.typography.titleMedium, color = TextSecondaryMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RESPONDING: AI has a reply — show confirm/amend options
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RespondingContent(
    message: String,
    currentQuestionHypothesis: String?,
    showAmendField: Boolean,
    amendText: String,
    showConfirmationActions: Boolean,
    isLifeStoryMode: Boolean,
    onConfirm: () -> Unit,
    onAmend: () -> Unit,
    onAmendTextChange: (String) -> Unit,
    onSubmitAmendment: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onTalkAgain: () -> Unit,
    onTypeInstead: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // AI companion icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = SecondaryGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🌿", fontSize = 40.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = TextPrimaryDark,
            lineHeight = 36.sp
        )

        // Show caregiver's hypothesis for confirmation
        if (currentQuestionHypothesis != null) {
            Spacer(Modifier.height(20.dp))
            Surface(color = TertiaryGreen, shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("👨‍👩‍👧", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Family said:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryMuted)
                        Text("\"$currentQuestionHypothesis\"", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = PrimaryGreen)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (!showAmendField && showConfirmationActions) {
            // Main action buttons
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Yes, that's right! ✓", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAmend,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Let me correct this", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else if (showAmendField) {
            // Amendment input
            Text("What is the correct answer?", style = MaterialTheme.typography.titleMedium, color = PrimaryGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amendText,
                onValueChange = onAmendTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type or speak your answer...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = SecondaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSubmitAmendment,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = amendText.isNotBlank()
            ) {
                Text("Save my answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            if (isLifeStoryMode) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) { Text("Next Question →", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onTalkAgain,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Speak", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTypeInstead,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Type", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (showConfirmationActions || showAmendField) {
            TextButton(onClick = onSkip) {
                Text("Ask me later", color = TextSecondaryMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPLETED: Session done
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompletedContent(
    message: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("🌱", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = PrimaryGreen, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Continue conversation →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Go back home", color = TextSecondaryMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NO PENDING QUESTIONS: Vault is exhausted — patient-driven mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoPendingContent(
    onShareMemory: (String) -> Unit,
    onBack: () -> Unit
) {
    var memoryText by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("🌸", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text("I am here for you! 🌿", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Spacer(Modifier.height(12.dp))
        Text("You can ask me to set a reminder, play a cognitive game, or simply share a memory with me.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = TextSecondaryMuted)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = memoryText,
            onValueChange = { memoryText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Set a reminder or share a thought...") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen, unfocusedBorderColor = SecondaryGreen),
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (memoryText.isNotBlank()) onShareMemory(memoryText) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            enabled = memoryText.isNotBlank()
        ) {
            Text("Send to Companion 🌿", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Go back home", color = TextSecondaryMuted) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, color = ErrorRed)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text("Go back home") }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hint Chips (always visible during voice session)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoiceHintChips(
    onTypeInstead: () -> Unit,
    onSkip: (() -> Unit)?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Quick voice commands:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryMuted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val hints = if (onSkip != null) {
                listOf("\"Yes, correct\"", "\"Skip this\"", "\"I don't know\"")
            } else {
                listOf("\"Set a reminder\"", "\"Play a game\"", "\"Help me\"")
            }
            hints.forEach { hint ->
                Surface(color = TertiaryGreen, shape = RoundedCornerShape(50)) {
                    Text(hint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onTypeInstead) {
            Icon(Icons.Default.Create, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Type instead", color = TextSecondaryMuted, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulsingMicButton(onTap: () -> Unit, isPaused: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isPaused) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPaused) 0f else 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900), repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(SecondaryGreen.copy(alpha = ringAlpha))
        )
        // Mic button
        Surface(
            modifier = Modifier.size(120.dp).clickable { onTap() },
            shape = CircleShape,
            color = PrimaryGreen,
            shadowElevation = 16.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MicNone, contentDescription = "Microphone", tint = SurfaceWhite, modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
private fun SoundwaveAnimation(isActive: Boolean, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val bars = 5
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(60.dp)
    ) {
        for (i in 0 until bars) {
            val height by infiniteTransition.animateFloat(
                initialValue = 12f, targetValue = (30..55).random().toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + i * 100,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(if (isActive) height.dp else 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
