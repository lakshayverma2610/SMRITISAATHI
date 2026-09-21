package com.nercare.cogcare.presentation.games

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log

class RegionalSoundManager(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays a culturally familiar acoustic sound (like a Sitar or Flute chime)
     * when a game is successfully completed to promote emotional well-being.
     */
    fun playCulturalSuccessSound() {
        try {
            // For the hackathon prototype, we fallback to the default notification sound.
            // When the asset team provides the 'sitar_success.mp3', uncomment the line below
            // and place the file in the res/raw folder.
            // mediaPlayer = MediaPlayer.create(context, R.raw.sitar_success)
            
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context, defaultSoundUri)
            
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer?.start()
            Log.d("RegionalSoundManager", "Playing cultural success chime (Sitar/Flute)")
        } catch (e: Exception) {
            Log.e("RegionalSoundManager", "Error playing sound", e)
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
