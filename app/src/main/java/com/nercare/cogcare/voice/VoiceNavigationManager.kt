package com.nercare.cogcare.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VoiceNavigationManager handles:
 * 1. Text-to-Speech (TTS) output in selected locale (English, Assamese, Bengali)
 * 2. Speech-to-Text (STR) input for voice commands
 * 3. Command parsing to trigger navigation actions
 */
@Singleton
class VoiceNavigationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceNav"
    }

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val _voiceCommand = MutableStateFlow<VoiceCommand?>(null)
    val voiceCommand: StateFlow<VoiceCommand?> = _voiceCommand

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    enum class VoiceCommand {
        OPEN_GAMES,
        OPEN_REMINDERS,
        GO_HOME,
        READ_REMINDERS,
        CALL_CAREGIVER,
        NEXT,
        REPEAT,
        STOP
    }

    private val languageLocaleMap = mapOf(
        "en" to Locale.ENGLISH,
        "as" to Locale("as", "IN"),  // Assamese
        "bn" to Locale("bn", "IN")   // Bengali
    )

    fun initialize(language: String = "en") {
        val locale = languageLocaleMap[language] ?: Locale.ENGLISH
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = locale
                tts?.setSpeechRate(0.85f) // Slightly slower for elderly
                tts?.setPitch(1.0f)
                isTtsReady = true
                Log.d(TAG, "TTS initialized with locale: $locale")
            } else {
                Log.e(TAG, "TTS init failed with status: $status")
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready, retrying...")
            return
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onComplete?.invoke() }
            override fun onError(utteranceId: String?) {}
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cog_care_utterance")
    }

    fun startListening(language: String = "en") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }
            override fun onError(error: Int) {
                _isListening.value = false
                Log.e(TAG, "Speech error: $error")
            }
            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    parseCommand(matches.first())
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocaleMap[language]?.toLanguageTag() ?: "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    private fun parseCommand(text: String) {
        val lower = text.lowercase()
        val command = when {
            lower.contains("game") || lower.contains("play") || lower.contains("খেলা") -> VoiceCommand.OPEN_GAMES
            lower.contains("reminder") || lower.contains("medicine") || lower.contains("ওষুধ") -> VoiceCommand.OPEN_REMINDERS
            lower.contains("home") || lower.contains("main") -> VoiceCommand.GO_HOME
            lower.contains("read") || lower.contains("tell") -> VoiceCommand.READ_REMINDERS
            lower.contains("caregiver") || lower.contains("call") -> VoiceCommand.CALL_CAREGIVER
            lower.contains("next") || lower.contains("পরের") -> VoiceCommand.NEXT
            lower.contains("repeat") || lower.contains("again") -> VoiceCommand.REPEAT
            lower.contains("stop") || lower.contains("quit") -> VoiceCommand.STOP
            else -> null
        }
        _voiceCommand.value = command
    }

    fun greetPatient(patientName: String, language: String = "en") {
        val greeting = when (language) {
            "as" -> "নমস্কাৰ $patientName! আজি আপুনি কেনেকুৱা আছে?"  // Assamese
            "bn" -> "নমস্কার $patientName! আজকে আপনি কেমন আছেন?"         // Bengali
            else -> "Hello $patientName! How are you feeling today?"
        }
        speak(greeting)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        isTtsReady = false
    }
}
