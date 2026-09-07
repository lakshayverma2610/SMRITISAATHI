package com.nercare.cogcare.di

import com.google.ai.client.generativeai.GenerativeModel
import com.nercare.cogcare.ai.AiIntentEngine
import com.nercare.cogcare.ai.HybridAiEngine
import com.nercare.cogcare.ai.cloud.GeminiAiEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.nercare.cogcare.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        // Safe fallback if API key is not configured yet
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            "MOCK_KEY"
        }
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )
    }

    @Provides
    @Singleton
    fun provideAiIntentEngine(hybridAiEngine: HybridAiEngine): AiIntentEngine {
        return hybridAiEngine
    }
}
