package com.nercare.cogcare.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nercare.cogcare.ai.bhashini.BhashiniService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class VoiceReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bhashiniService: BhashiniService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val title = inputData.getString("title") ?: "Reminder"
        val patientName = inputData.getString("patientName") ?: ""
        
        val spokenText = if (patientName.isNotEmpty()) {
            "Pranam $patientName, it is time for $title."
        } else {
            "Pranam, it is time for $title."
        }

        try {
            // Attempt to get Bhashini TTS
            val audioData = bhashiniService.tts(spokenText, "as") // Defaulting to Assamese
            
            if (audioData != null && audioData.audioContent.isNotEmpty()) {
                playAudioBase64(audioData.audioContent)
            } else {
                Log.w("VoiceReminderWorker", "Bhashini TTS failed, fallback to native TTS could go here")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("VoiceReminderWorker", "Error playing voice reminder", e)
            Result.failure()
        }
    }

    private suspend fun playAudioBase64(base64Audio: String) {
        // Implementation for decoding base64 and playing it using MediaPlayer
        // For the sake of the prototype, we assume the base64 is a standard mp3/wav
        try {
            val audioBytes = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)
            val tempFile = java.io.File.createTempFile("reminder_audio", ".wav", applicationContext.cacheDir)
            tempFile.writeBytes(audioBytes)

            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
            }
            
            mediaPlayer.start()
            
            // Wait for it to finish
            kotlinx.coroutines.delay(mediaPlayer.duration.toLong() + 1000)
            mediaPlayer.release()
            tempFile.delete()
        } catch (e: Exception) {
            Log.e("VoiceReminderWorker", "Failed to play audio", e)
        }
    }
}
