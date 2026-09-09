package com.nercare.cogcare.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.Patient
import com.nercare.cogcare.domain.model.SocialProfile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseService"
        private const val TIMEOUT_MS = 2500L
        private const val COLLECTION_PATIENTS = "patients"
        private const val COLLECTION_SESSIONS = "game_sessions"
        private const val COLLECTION_SOCIAL_PROFILES = "social_profiles"
        private const val COLLECTION_USERNAMES = "patient_usernames"
    }

    suspend fun syncSocialProfile(profile: SocialProfile) {
        try {
            withTimeoutOrNull(TIMEOUT_MS) {
                firestore.collection(COLLECTION_SOCIAL_PROFILES).document(profile.patientId).set(profile).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync social profile: ${e.message}")
        }
    }

    suspend fun fetchSocialProfiles(): List<SocialProfile> = try {
        withTimeoutOrNull(TIMEOUT_MS) {
            firestore.collection(COLLECTION_SOCIAL_PROFILES).get().await().toObjects(SocialProfile::class.java)
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch social profiles: ${e.message}")
        emptyList()
    }

    suspend fun syncPatient(patient: Patient): Boolean {
        return try {
            val result = withTimeoutOrNull(TIMEOUT_MS) {
                firestore.collection(COLLECTION_PATIENTS)
                    .document(patient.id)
                    .set(patient)
                    .await()
            }
            result != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync patient: ${e.message}")
            false
        }
    }

    /** Atomically reserves a normalized username and creates its patient record. */
    suspend fun createPatient(patient: Patient): Boolean {
        val normalized = patient.username.trim().lowercase()
        return try {
            firestore.runTransaction { transaction ->
                val usernameRef = firestore.collection(COLLECTION_USERNAMES).document(normalized)
                if (transaction.get(usernameRef).exists()) {
                    throw IllegalStateException("Username is already taken")
                }
                transaction.set(usernameRef, mapOf("patientId" to patient.id, "caregiverId" to patient.caregiverId))
                transaction.set(firestore.collection(COLLECTION_PATIENTS).document(patient.id), patient)
            }.await()
            true
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create patient: ${e.message}")
            throw e
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean =
        !firestore.collection(COLLECTION_USERNAMES).document(username.trim().lowercase()).get().await().exists()

    suspend fun deletePatient(patient: Patient) {
        val sessions = firestore.collection(COLLECTION_SESSIONS)
            .whereEqualTo("patientId", patient.id)
            .get().await()
        val batch = firestore.batch()
        batch.delete(firestore.collection(COLLECTION_PATIENTS).document(patient.id))
        batch.delete(firestore.collection(COLLECTION_SOCIAL_PROFILES).document(patient.id))
        batch.delete(firestore.collection(COLLECTION_USERNAMES).document(patient.username.trim().lowercase()))
        sessions.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun fetchPatientsForCaregiver(caregiverId: String): List<Patient> = try {
        withTimeoutOrNull(TIMEOUT_MS) {
            firestore.collection(COLLECTION_PATIENTS)
                .whereEqualTo("caregiverId", caregiverId)
                .get().await().toObjects(Patient::class.java)
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch caregiver patients: ${e.message}")
        emptyList()
    }

    suspend fun syncGameSessions(sessions: List<GameSession>): Boolean {
        return try {
            val result = withTimeoutOrNull(TIMEOUT_MS) {
                val batch = firestore.batch()
                sessions.forEach { session ->
                    val ref = firestore.collection(COLLECTION_SESSIONS).document(session.id)
                    batch.set(ref, session)
                }
                batch.commit().await()
            }
            result != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync sessions: ${e.message}")
            false
        }
    }

    suspend fun fetchPatientSessions(patientId: String, limit: Long = 50): List<GameSession> {
        return try {
            withTimeoutOrNull(TIMEOUT_MS) {
                firestore.collection(COLLECTION_SESSIONS)
                    .whereEqualTo("patientId", patientId)
                    .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()
                    .toObjects(GameSession::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch sessions: ${e.message}")
            emptyList()
        }
    }
}

