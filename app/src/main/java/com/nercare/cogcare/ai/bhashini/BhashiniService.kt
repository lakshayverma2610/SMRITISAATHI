package com.nercare.cogcare.ai.bhashini

import android.util.Log
import com.nercare.cogcare.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface BhashiniApi {
    @POST("v1/pipeline")
    suspend fun runPipeline(
        @Header("Authorization") authHeader: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: BhashiniPipelineRequest
    ): BhashiniResponse
}

@Singleton
class BhashiniService @Inject constructor() {

    private val api: BhashiniApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            // Using a standard Bhashini base URL. In reality, you may need to call a discovery API first to get the inference URL.
            .baseUrl("https://dhruva-api.bhashini.gov.in/services/inference/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(BhashiniApi::class.java)
    }

    private val authHeader = BuildConfig.BHASHINI_API_KEY // Assumes keys are like "ApiKey YOUR_KEY"

    suspend fun asr(audioData: BhashiniAudioData, languageCode: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = BhashiniPipelineRequest(
                pipelineTasks = listOf(
                    PipelineTask(
                        taskType = "asr",
                        config = PipelineConfig(
                            language = LanguageConfig(sourceLanguage = languageCode)
                        )
                    )
                ),
                inputData = InputData(audio = listOf(audioData))
            )
            val response = api.runPipeline(authHeader, request = request)
            response.pipelineResponse.firstOrNull()?.output?.firstOrNull()?.source
        } catch (e: Exception) {
            Log.e("BhashiniService", "ASR failed", e)
            null
        }
    }

    suspend fun nmt(text: String, sourceLang: String, targetLang: String = "en"): String? = withContext(Dispatchers.IO) {
        try {
            val request = BhashiniPipelineRequest(
                pipelineTasks = listOf(
                    PipelineTask(
                        taskType = "translation",
                        config = PipelineConfig(
                            language = LanguageConfig(sourceLanguage = sourceLang, targetLanguage = targetLang)
                        )
                    )
                ),
                inputData = InputData(input = listOf(BhashiniTextData(source = text)))
            )
            val response = api.runPipeline(authHeader, request = request)
            response.pipelineResponse.firstOrNull()?.output?.firstOrNull()?.target
        } catch (e: Exception) {
            Log.e("BhashiniService", "NMT failed", e)
            null
        }
    }

    suspend fun tts(text: String, targetLang: String): BhashiniAudioData? = withContext(Dispatchers.IO) {
        try {
            val request = BhashiniPipelineRequest(
                pipelineTasks = listOf(
                    PipelineTask(
                        taskType = "tts",
                        config = PipelineConfig(
                            language = LanguageConfig(sourceLanguage = targetLang)
                        )
                    )
                ),
                inputData = InputData(input = listOf(BhashiniTextData(source = text)))
            )
            val response = api.runPipeline(authHeader, request = request)
            response.pipelineResponse.firstOrNull()?.output?.firstOrNull()?.audio?.firstOrNull()
        } catch (e: Exception) {
            Log.e("BhashiniService", "TTS failed", e)
            null
        }
    }
}
