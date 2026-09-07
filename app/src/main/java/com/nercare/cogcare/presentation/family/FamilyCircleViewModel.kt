package com.nercare.cogcare.presentation.family

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.FamilyRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class FamilyCircleUiState(
    val members: List<FamilyMember> = emptyList(),
    val playingMemberId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class FamilyCircleViewModel @Inject constructor(
    private val lifeStoryRepository: LifeStoryRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyCircleUiState())
    val uiState: StateFlow<FamilyCircleUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                isTtsReady = true
            }
        }
    }

    fun load(patientId: String) {
        viewModelScope.launch {
            lifeStoryRepository.observeAllFamilyMembers(patientId)
                .collect { list ->
                    _uiState.update { it.copy(members = list, isLoading = false) }
                }
        }
    }

    fun toggleVoiceNote(member: FamilyMember) {
        if (_uiState.value.playingMemberId == member.id) {
            stopAudio()
            return
        }

        stopAudio()
        _uiState.update { it.copy(playingMemberId = member.id) }

        val uriString = member.voiceNoteUri
        if (!uriString.isNullOrBlank()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(appContext, Uri.parse(uriString))
                    prepareAsync()
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        stopAudio()
                    }
                    setOnErrorListener { _, _, _ ->
                        playFallbackGreeting(member)
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("FamilyCircleVM", "Error playing media, falling back to TTS", e)
                playFallbackGreeting(member)
            }
        } else {
            playFallbackGreeting(member)
        }
    }

    private fun playFallbackGreeting(member: FamilyMember) {
        val greeting = if (!member.favouriteSharedMemory.isNullOrBlank()) {
            "Hi! Remember, ${member.favouriteSharedMemory}. Sending you so much love!"
        } else {
            "Hello from ${member.fullName}! Wishing you a peaceful day. Always thinking of you!"
        }

        if (isTtsReady && tts != null) {
            tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "FamilyVoice_${member.id}")
            viewModelScope.launch {
                kotlinx.coroutines.delay(5000L)
                if (_uiState.value.playingMemberId == member.id) {
                    _uiState.update { it.copy(playingMemberId = null) }
                }
            }
        } else {
            _uiState.update { it.copy(playingMemberId = null) }
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            tts?.stop()
        } catch (_: Exception) {}

        _uiState.update { it.copy(playingMemberId = null) }
    }

    fun addFamilyMember(
        patientId: String,
        fullName: String,
        relation: FamilyRelation,
        phone: String = "",
        favouriteMemory: String = "",
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val member = FamilyMember(
                patientId = patientId,
                fullName = fullName.trim(),
                relation = relation,
                primaryPhone = phone.trim().ifBlank { null },
                favouriteSharedMemory = favouriteMemory.trim().ifBlank { null },
                mainPhotoUri = photoUri
            )
            lifeStoryRepository.saveFamilyMember(member)
        }
    }

    fun deleteFamilyMember(id: Long) {
        viewModelScope.launch {
            lifeStoryRepository.deleteFamilyMember(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
