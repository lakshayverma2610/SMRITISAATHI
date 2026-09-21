package com.nercare.cogcare.ai.gemini

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nercare.cogcare.BuildConfig
import com.nercare.cogcare.presentation.games.wordassoc.WordPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiContentGenerator @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /**
     * Generates a dynamic WordPair for the Word Association Game using Gemini API.
     * Throws an exception if offline or API fails.
     */
    suspend fun generateWordPair(): WordPair = withContext(Dispatchers.IO) {
        val prompt = """
            Generate 1 word association pair suitable for a cognitive therapy patient in India.
            Return ONLY a valid JSON object in this exact format:
            {
                "prompt": "Tea",
                "answer": "Cup",
                "decoys": ["Shoe", "Tree", "Car", "Sun", "Plate"]
            }
            Do not include Markdown formatting or backticks. Just the raw JSON.
        """.trimIndent()

        val response = model.generateContent(prompt)
        val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() 
            ?: throw Exception("Empty response from Gemini")

        val json = JSONObject(jsonText)
        val decoysArray = json.getJSONArray("decoys")
        val decoys = mutableListOf<String>()
        for (i in 0 until decoysArray.length()) {
            decoys.add(decoysArray.getString(i))
        }

        WordPair(
            prompt = json.getString("prompt"),
            answer = json.getString("answer"),
            decoys = decoys
        )
    }

    /**
     * Generates a dynamic daily routine sequence using Gemini API.
     * Throws an exception if offline or API fails.
     */
    suspend fun generateDailyRoutine(stepCount: Int): List<String> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate a $stepCount-step daily routine for an elderly patient living in India.
            The steps MUST be in strict chronological order.
            Return ONLY a valid JSON array of strings. 
            Example: ["Wake up", "Drink Chai", "Read Newspaper", "Take Medicine", "Eat Lunch"]
            Do not include Markdown formatting or backticks. Just the raw JSON array.
        """.trimIndent()

        val response = model.generateContent(prompt)
        val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim()
            ?: throw Exception("Empty response from Gemini")

        val jsonArray = JSONArray(jsonText)
        val routine = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            routine.add(jsonArray.getString(i))
        }

        if (routine.size < stepCount) {
            throw Exception("Gemini returned too few steps")
        }
        
        routine.take(stepCount)
    }
}
