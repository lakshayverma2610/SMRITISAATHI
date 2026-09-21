package com.nercare.cogcare.ai

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    companion object {
        const val MODEL_FILENAME = "adaptive_difficulty.tflite"
        // Replace this with your actual Firebase Storage download URL once uploaded
        private const val MODEL_URL = "https://storage.googleapis.com/your-bucket/adaptive_difficulty.tflite"
        private const val TAG = "ModelDownloadManager"
    }

    val modelFile: File
        get() = File(context.filesDir, MODEL_FILENAME)

    suspend fun downloadModelIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (modelFile.exists()) {
            Log.d(TAG, "Model already exists at ${modelFile.absolutePath}")
            return@withContext true
        }

        Log.d(TAG, "Model not found. Downloading from $MODEL_URL")
        try {
            val request = Request.Builder().url(MODEL_URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to download model: ${response.code}")
                return@withContext false
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Model successfully downloaded and saved to ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            // If download fails, delete any partial file
            if (modelFile.exists()) {
                modelFile.delete()
            }
            false
        }
    }
}
