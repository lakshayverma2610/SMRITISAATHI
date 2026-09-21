package com.nercare.cogcare.ai

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import com.nercare.cogcare.domain.model.CognitiveStage
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * AdaptiveDifficultyEngine — LiteRT-powered cognitive difficulty recommender.
 *
 * Uses a LiteRT (formerly TensorFlow Lite) model if available in assets
 * (assets/adaptive_difficulty.tflite). Falls back to the rule-based algorithm
 * if the model file is absent or fails to load (safe offline-first design).
 *
 * Model input tensor  : [1, 5] float32
 *   [0] avg_accuracy_last3     (0.0–1.0)
 *   [1] avg_response_ms_norm   (0.0–1.0, clamped at 10000ms)
 *   [2] cognitive_stage        (0=EARLY, 1=MILD, 2=MODERATE, 3=SEVERE)
 *   [3] current_difficulty     (1–5, normalised /5)
 *   [4] hour_of_day_norm       (0.0–1.0)
 *
 * Model output tensor : [1, 1] float32
 *   [0] recommended_difficulty (1.0–5.0)
 *
 * Difficulty levels: 1 (easiest) → 5 (hardest)
 */
@Singleton
class AdaptiveDifficultyEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AdaptiveDifficultyEngine"
        private const val MODEL_ASSET = "adaptive_difficulty.tflite"
        private const val INPUT_SIZE = 5
        private const val MAX_RESPONSE_MS = 10_000f
    }

    data class DifficultyRecommendation(
        val level: Int, // 1–5
        val nextGame: GameType,
        val encouragementMessage: String,
        val shouldTakeBreak: Boolean
    )

    /** LiteRT interpreter — null if model not available (falls back to rules). */
    private val interpreter: Interpreter? by lazy { loadInterpreter() }

    private fun loadInterpreter(): Interpreter? {
        return try {
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            try {
                val model = loadModelFromFile(MODEL_ASSET)
                Interpreter(model, options).also {
                    Log.i(TAG, "LiteRT model loaded from internal storage: $MODEL_ASSET")
                }
            } catch (e: Exception) {
                val model = loadModelFromAssets(MODEL_ASSET)
                Interpreter(model, options).also {
                    Log.i(TAG, "LiteRT model loaded from assets: $MODEL_ASSET")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "LiteRT model not available, using rule-based fallback: ${e.message}")
            null
        }
    }

    private fun loadModelFromFile(filename: String): MappedByteBuffer {
        val file = java.io.File(context.filesDir, filename)
        if (!file.exists()) throw java.io.FileNotFoundException("Model not found in internal storage")
        val inputStream = FileInputStream(file)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            file.length()
        )
    }

    private fun loadModelFromAssets(filename: String): MappedByteBuffer {
        val assetFd = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFd.fileDescriptor)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFd.startOffset,
            assetFd.declaredLength
        )
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Calculate recommended difficulty for the next session.
     * Uses LiteRT if model is available, otherwise uses rule-based algorithm.
     */
    fun recommend(
        recentSessions: List<GameSession>,
        cognitiveStage: CognitiveStage,
        currentHour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): DifficultyRecommendation {

        if (recentSessions.isEmpty()) {
            return DifficultyRecommendation(
                level = baselineLevel(cognitiveStage),
                nextGame = GameType.MEMORY_CARD,
                encouragementMessage = "Let's start with something fun! 🌟",
                shouldTakeBreak = false
            )
        }

        val last3 = recentSessions.take(3)
        val avgAccuracy = last3.map { it.accuracyPercent }.average().toFloat()
        val avgResponseMs = last3.map { it.avgResponseTimeMs }.average().toLong()
        val lastDifficulty = last3.first().difficultyLevel

        val recommendedLevel = interpreter
            ?.let { runLiteRTInference(it, avgAccuracy, avgResponseMs, cognitiveStage, lastDifficulty, currentHour) }
            ?: computeRuleBasedLevel(avgAccuracy, avgResponseMs, cognitiveStage, lastDifficulty, currentHour)

        val shouldBreak = last3.size >= 3 && isAccuracyDeclining(last3)
        val lastGame = last3.first().gameType
        val nextGame = getNextGame(lastGame, avgAccuracy)
        val message = buildEncouragementMessage(avgAccuracy)

        return DifficultyRecommendation(
            level = recommendedLevel,
            nextGame = nextGame,
            encouragementMessage = message,
            shouldTakeBreak = shouldBreak
        )
    }

    /**
     * Legacy helper method for ViewModels that directly pass calculated metrics.
     */
    fun recommendDifficulty(
        avgAccuracy: Float,
        avgResponseMs: Long,
        cognitiveStageInt: Int,
        lastDifficulty: Int
    ): Int {
        val stage = CognitiveStage.values().getOrNull(cognitiveStageInt) ?: CognitiveStage.MILD
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return interpreter
            ?.let { runLiteRTInference(it, avgAccuracy, avgResponseMs, stage, lastDifficulty, currentHour) }
            ?: computeRuleBasedLevel(avgAccuracy, avgResponseMs, stage, lastDifficulty, currentHour)
    }

    // ─── LiteRT Inference ────────────────────────────────────────────────────

    private fun runLiteRTInference(
        interpreter: Interpreter,
        avgAccuracy: Float,
        avgResponseMs: Long,
        cognitiveStage: CognitiveStage,
        lastDifficulty: Int,
        currentHour: Int
    ): Int {
        return try {
            // Build input tensor [1, 5] float32
            val inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4).apply {
                order(ByteOrder.nativeOrder())
                putFloat(avgAccuracy / 100f)                           // accuracy (0–1)
                putFloat((avgResponseMs.toFloat() / MAX_RESPONSE_MS).coerceIn(0f, 1f))
                putFloat(cognitiveStage.ordinal / 3f)                  // stage (0–1)
                putFloat(lastDifficulty / 5f)                          // difficulty (0–1)
                putFloat(currentHour / 23f)                            // hour (0–1)
            }

            // Build output tensor [1, 1] float32
            val outputBuffer = ByteBuffer.allocateDirect(1 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val rawOutput = outputBuffer.float        // e.g. 2.7
            rawOutput.roundToInt().coerceIn(1, 5)
        } catch (e: Exception) {
            Log.e(TAG, "LiteRT inference failed, falling back to rules: ${e.message}")
            computeRuleBasedLevel(avgAccuracy, avgResponseMs, cognitiveStage, lastDifficulty, currentHour)
        }
    }

    // ─── Rule-based fallback ─────────────────────────────────────────────────

    private fun computeRuleBasedLevel(
        avgAccuracy: Float,
        avgResponseMs: Long,
        cognitiveStage: CognitiveStage,
        lastDifficulty: Int,
        currentHour: Int
    ): Int {
        val rawDifficulty = when {
            avgAccuracy >= 85f && avgResponseMs < 3000L -> lastDifficulty + 1  // Too easy
            avgAccuracy >= 70f && avgResponseMs < 5000L -> lastDifficulty      // Just right
            avgAccuracy >= 55f                          -> lastDifficulty      // Acceptable
            else                                        -> lastDifficulty - 1  // Too hard
        }
        val stageCapped = (rawDifficulty * cognitiveStage.difficultyMultiplier).roundToInt()
        val clamped = max(1, min(5, stageCapped))
        // Afternoon sessions slightly easier (post-lunch dip)
        return if (currentHour in 13..15) max(1, clamped - 1) else clamped
    }

    // ─── Game-specific helpers ────────────────────────────────────────────────

    /** Grid size for memory card game. Difficulty 1=2x2 … 5=4x5 */
    fun getMemoryGridSize(difficulty: Int): Pair<Int, Int> = when (difficulty) {
        1    -> Pair(2, 2)
        2    -> Pair(2, 3)
        3    -> Pair(3, 4)
        4    -> Pair(4, 4)
        else -> Pair(4, 5)
    }

    /** Sequence length for sequence recall game. */
    fun getSequenceLength(difficulty: Int): Int = when (difficulty) {
        1    -> 3
        2    -> 4
        3    -> 5
        4    -> 7
        else -> 9
    }

    /** Number of pattern choices for pattern matching. */
    fun getPatternChoices(difficulty: Int): Int = when (difficulty) {
        1    -> 2
        2    -> 3
        3    -> 4
        4    -> 5
        else -> 6
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun baselineLevel(stage: CognitiveStage): Int = when (stage) {
        CognitiveStage.EARLY    -> 3
        CognitiveStage.MILD     -> 2
        CognitiveStage.MODERATE -> 2
        CognitiveStage.SEVERE   -> 1
    }

    private fun isAccuracyDeclining(sessions: List<GameSession>): Boolean {
        if (sessions.size < 2) return false
        val sorted = sessions.sortedBy { it.completedAt }
        return sorted.last().accuracyPercent < sorted.first().accuracyPercent - 15f
    }

    private fun getNextGame(lastGame: GameType, accuracy: Float): GameType {
        val games = GameType.values()
        val currentIndex = games.indexOf(lastGame)
        return if (accuracy < 50f) lastGame
        else games[(currentIndex + 1) % games.size]
    }

    private fun buildEncouragementMessage(avgAccuracy: Float): String = when {
        avgAccuracy >= 80f -> "Excellent work! You're doing amazingly! 🎉"
        avgAccuracy >= 65f -> "Good job! Keep it up! 👍"
        avgAccuracy >= 50f -> "You're doing well. Let's try again! 💪"
        else               -> "Let's take it slow and steady. You can do it! 🌸"
    }
}
