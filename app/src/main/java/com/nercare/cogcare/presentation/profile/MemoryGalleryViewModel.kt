package com.nercare.cogcare.presentation.profile

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.FamilyRelation
import com.nercare.cogcare.domain.model.LifeMemoryNode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

enum class VoicePlaybackState { IDLE, LOADING, PLAYING }
enum class RecordingState { IDLE, RECORDING, SAVED, ERROR }

data class MemoryGalleryUiState(
    val memories: List<LifeMemoryNode> = emptyList(),
    val familyMembers: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = true,
    val playingMemberId: Long? = null,
    val voicePlaybackState: VoicePlaybackState = VoicePlaybackState.IDLE,
    val recordingState: RecordingState = RecordingState.IDLE,
    val recordingMemberId: Long? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class MemoryGalleryViewModel @Inject constructor(
    private val lifeStoryRepository: LifeStoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryGalleryUiState())
    val uiState: StateFlow<MemoryGalleryUiState> = _uiState.asStateFlow()
    private var loadedPatientId: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale("en", "IN"))
                }
                isTtsReady = true
            }
        }
    }

    fun load(patientId: String) {
        if (loadedPatientId == patientId) return
        loadedPatientId = patientId
        viewModelScope.launch {
            combine(
                lifeStoryRepository.observeAllMemoryNodes(patientId),
                lifeStoryRepository.observeAllFamilyMembers(patientId)
            ) { memories, family -> memories to family }
                .collect { (memories, family) ->
                    _uiState.update { it.copy(memories = memories, familyMembers = family, isLoading = false) }
                }
        }
    }

    fun toggleVoice(member: FamilyMember) {
        if (_uiState.value.playingMemberId == member.id) { stopPlayback(); return }
        stopPlayback()
        _uiState.update { it.copy(playingMemberId = member.id, voicePlaybackState = VoicePlaybackState.LOADING) }
        val uriString = member.voiceNoteUri
        if (!uriString.isNullOrBlank()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA).build())
                    setDataSource(context, Uri.parse(uriString))
                    prepareAsync()
                    setOnPreparedListener { _uiState.update { it.copy(voicePlaybackState = VoicePlaybackState.PLAYING) }; start() }
                    setOnCompletionListener { stopPlayback() }
                    setOnErrorListener { _, _, _ -> playTtsFallback(member); true }
                }
            } catch (e: Exception) { Log.e("MemGalleryVM", "MP error", e); playTtsFallback(member) }
        } else { playTtsFallback(member) }
    }

    private fun playTtsFallback(member: FamilyMember) {
        if (!isTtsReady || tts == null) {
            stopPlayback()
            _uiState.update { it.copy(snackbarMessage = "No voice recorded yet. Tap 'Record Voice' to add one.") }
            return
        }
        val greeting = if (!member.favouriteSharedMemory.isNullOrBlank())
            "${member.fullName} says: ${member.favouriteSharedMemory}"
        else "Hello! This is ${member.fullName}, your ${member.relation.displayLabel}. Wishing you love and peace today!"
        _uiState.update { it.copy(voicePlaybackState = VoicePlaybackState.PLAYING) }
        tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "fallback_${member.id}")
        viewModelScope.launch {
            kotlinx.coroutines.delay(6000L)
            if (_uiState.value.playingMemberId == member.id) stopPlayback()
        }
    }

    fun stopPlayback() {
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        try { tts?.stop() } catch (_: Exception) {}
        _uiState.update { it.copy(playingMemberId = null, voicePlaybackState = VoicePlaybackState.IDLE) }
    }

    fun startRecording(member: FamilyMember) {
        if (_uiState.value.recordingState == RecordingState.RECORDING) return
        stopPlayback()
        val file = File(context.filesDir, "voice_${member.id}_${System.currentTimeMillis()}.m4a")
        currentRecordingFile = file
        try {
            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare(); start()
            }
            _uiState.update { it.copy(recordingState = RecordingState.RECORDING, recordingMemberId = member.id) }
        } catch (e: Exception) {
            Log.e("MemGalleryVM", "Record failed", e)
            _uiState.update { it.copy(recordingState = RecordingState.ERROR, snackbarMessage = "Could not start recording. Check mic permission.") }
        }
    }

    fun stopRecording(member: FamilyMember) {
        try { mediaRecorder?.stop(); mediaRecorder?.release() } catch (e: Exception) {
            Log.e("MemGalleryVM", "Stop recording failed", e)
            mediaRecorder = null
            _uiState.update { it.copy(recordingState = RecordingState.ERROR, recordingMemberId = null, snackbarMessage = "Recording failed. Try again.") }
            return
        }
        mediaRecorder = null
        val file = currentRecordingFile
        if (file != null && file.exists()) {
            viewModelScope.launch {
                lifeStoryRepository.updateFamilyMemberVoiceNote(member.id, file.toURI().toString())
                _uiState.update { it.copy(recordingState = RecordingState.SAVED, recordingMemberId = null, snackbarMessage = "Voice recorded and saved!") }
            }
        } else {
            _uiState.update { it.copy(recordingState = RecordingState.IDLE, recordingMemberId = null) }
        }
        currentRecordingFile = null
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
        if (_uiState.value.recordingState == RecordingState.SAVED || _uiState.value.recordingState == RecordingState.ERROR) {
            _uiState.update { it.copy(recordingState = RecordingState.IDLE) }
        }
    }

    fun addFamilyMember(fullName: String, relationLabel: String, photoUri: String? = null) {
        val patientId = loadedPatientId ?: return
        viewModelScope.launch {
            val relation = FamilyRelation.entries
                .firstOrNull { it.displayLabel.equals(relationLabel.trim(), ignoreCase = true) }
                ?: FamilyRelation.OTHER
            lifeStoryRepository.saveFamilyMember(FamilyMember(
                patientId = patientId, fullName = fullName.trim(),
                relation = relation, mainPhotoUri = photoUri
            ))
        }
    }

    override fun onCleared() {
        stopPlayback()
        try { mediaRecorder?.stop(); mediaRecorder?.release() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
        super.onCleared()
    }
}
