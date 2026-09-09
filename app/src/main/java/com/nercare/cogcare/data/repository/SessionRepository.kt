package com.nercare.cogcare.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "login_session")

data class SavedSession(val role: String, val patientId: String? = null)

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val ROLE = stringPreferencesKey("role")
        val PATIENT_ID = stringPreferencesKey("patient_id")
        const val CAREGIVER = "caregiver"
        const val PATIENT = "patient"
    }

    suspend fun saveCaregiverSession() = context.sessionDataStore.edit {
        it[ROLE] = CAREGIVER
        it.remove(PATIENT_ID)
    }

    suspend fun savePatientSession(patientId: String) = context.sessionDataStore.edit {
        it[ROLE] = PATIENT
        it[PATIENT_ID] = patientId
    }

    suspend fun getSession(): SavedSession? {
        val preferences = context.sessionDataStore.data.first()
        return when (preferences[ROLE]) {
            CAREGIVER -> SavedSession(CAREGIVER)
            PATIENT -> preferences[PATIENT_ID]?.takeIf(String::isNotBlank)?.let { SavedSession(PATIENT, it) }
            else -> null
        }
    }

    suspend fun clear() = context.sessionDataStore.edit { it.clear() }
}
