package com.nercare.cogcare.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nercare.cogcare.data.repository.GameRepository
import com.nercare.cogcare.data.local.dao.GameContentDao
import com.nercare.cogcare.ai.gemini.GeminiContentGenerator
import com.nercare.cogcare.data.local.entities.GameContentEntity
import com.nercare.cogcare.domain.model.GameType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * SyncWorker runs in background via WorkManager to sync unsynced game sessions
 * to Firebase Firestore when connectivity is available.
 *
 * Scheduled as periodic work (every 30 minutes) with NETWORK_CONNECTED constraint.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val gameRepository: GameRepository,
    private val gameContentDao: GameContentDao,
    private val geminiContentGenerator: GeminiContentGenerator
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")
        return try {
            // 1. Sync game sessions to Firestore
            gameRepository.syncUnsyncedSessions()
            
            // 2. Prefetch Gemini Content (Phase 6)
            prefetchGameContent()

            Log.d(TAG, "SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun prefetchGameContent() {
        try {
            // Check Word Association Cache
            if (gameContentDao.getUnusedCount(GameType.WORD_ASSOCIATION.name) < 20) {
                val words = mutableListOf<GameContentEntity>()
                for (i in 1..5) { // Fetch 5 items per sync to avoid timeouts
                    val pair = geminiContentGenerator.generateWordPair()
                    words.add(GameContentEntity(gameType = GameType.WORD_ASSOCIATION.name, contentJson = Json.encodeToString(pair)))
                }
                gameContentDao.insertAll(words)
            }

            // Check Daily Routine Cache
            if (gameContentDao.getUnusedCount(GameType.DAILY_ROUTINE.name) < 20) {
                val routines = mutableListOf<GameContentEntity>()
                for (i in 1..5) {
                    val routine = geminiContentGenerator.generateDailyRoutine(5)
                    routines.add(GameContentEntity(gameType = GameType.DAILY_ROUTINE.name, contentJson = Json.encodeToString(routine)))
                }
                gameContentDao.insertAll(routines)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Prefetching failed, but that's okay: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "CogCareSyncWork"

        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun syncNow(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueue(syncRequest)
        }
    }
}
