package com.nercare.cogcare.data.repository

import com.nercare.cogcare.data.local.dao.PatientDao
import android.util.Log
import com.nercare.cogcare.data.local.dao.PatientCredentialDao
import com.nercare.cogcare.data.local.dao.LifeMemoryNodeDao
import com.nercare.cogcare.data.local.dao.FamilyMemberDao
import com.nercare.cogcare.data.local.dao.GameSessionDao
import com.nercare.cogcare.data.local.dao.ReminderDao
import com.nercare.cogcare.data.local.entities.PatientCredentialEntity
import com.nercare.cogcare.data.local.entities.toEntity
import com.nercare.cogcare.data.remote.FirebaseService
import com.nercare.cogcare.domain.model.Patient
import com.nercare.cogcare.domain.model.SocialProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val credentialDao: PatientCredentialDao,
    private val memoryNodeDao: LifeMemoryNodeDao,
    private val familyMemberDao: FamilyMemberDao,
    private val gameSessionDao: GameSessionDao,
    private val reminderDao: ReminderDao,
    private val firebaseService: FirebaseService
) {
    fun getAllPatients(): Flow<List<Patient>> =
        patientDao.getAllPatients().map { list -> list.map { it.toDomain() } }

    fun getPatientsForCaregiver(caregiverId: String): Flow<List<Patient>> =
        patientDao.getPatientsForCaregiver(caregiverId).map { list -> list.map { it.toDomain() } }

    suspend fun refreshPatientsForCaregiver(caregiverId: String) {
        firebaseService.fetchPatientsForCaregiver(caregiverId).forEach { patientDao.upsertPatient(it.toEntity()) }
    }

    suspend fun getAllPatientsSync(): List<Patient> =
        patientDao.getAllPatientsSync().map { it.toDomain() }

    fun observePatient(id: String): Flow<Patient?> =
        patientDao.observePatient(id).map { it?.toDomain() }

    suspend fun getPatientById(id: String): Patient? =
        patientDao.getPatientById(id)?.toDomain()

    suspend fun savePatient(patient: Patient): Patient {
        val toSave = if (patient.id.isEmpty()) {
            patient.copy(id = generatePatientId())
        } else patient
        patientDao.upsertPatient(toSave.toEntity())
        try {
            firebaseService.syncPatient(toSave)
        } catch (e: Exception) {
            Log.e("PatientRepository", "Failed to sync patient to remote: ${e.message}")
        }
        return toSave
    }

    suspend fun savePatientWithCredentials(patient: Patient): Pair<Patient, String> {
        return savePatientWithCredentials(patient, generatePassword())
    }

    suspend fun savePatientWithCredentials(patient: Patient, password: String): Pair<Patient, String> {
        val normalizedUsername = patient.username.trim().lowercase()
        require(normalizedUsername.matches(Regex("[a-z0-9._-]{3,30}"))) {
            "Username must be 3–30 characters and use only letters, numbers, dot, underscore or hyphen"
        }
        require(password.length >= 6) { "Patient password must contain at least 6 characters" }
        if (patientDao.getPatientByUsername(normalizedUsername) != null) error("Username is already taken")
        val saved = patient.copy(
            id = patient.id.ifBlank { generatePatientId() },
            username = normalizedUsername
        )
        firebaseService.createPatient(saved)
        patientDao.upsertPatient(saved.toEntity())
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        credentialDao.upsert(
            PatientCredentialEntity(
                patientId = saved.id,
                salt = Base64.getEncoder().encodeToString(salt),
                passwordHash = hashPassword(password, salt, 120_000)
            )
        )
        val publicInterests = listOf(saved.hobbies, saved.favoriteMusic, saved.favoriteFoods, saved.favoriteActivities, saved.profession)
            .flatMap { it.split(',', ';', '/', '\n') }.map(String::trim).filter { it.length >= 3 }.distinct().take(30)
        try {
            firebaseService.syncSocialProfile(
                SocialProfile(saved.id, saved.preferredName.ifBlank { saved.name }, saved.profileImageUrl, publicInterests)
            )
        } catch (e: Exception) {
            Log.e("PatientRepository", "Failed to sync social profile to remote: ${e.message}")
        }
        return saved to password
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val normalized = username.trim().lowercase()
        if (!normalized.matches(Regex("[a-z0-9._-]{3,30}"))) return false
        if (patientDao.getPatientByUsername(normalized) != null) return false
        return firebaseService.isUsernameAvailable(normalized)
    }

    suspend fun resetPatientPassword(patientId: String): String {
        val newPassword = generatePassword()
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        credentialDao.upsert(
            PatientCredentialEntity(
                patientId = patientId,
                salt = Base64.getEncoder().encodeToString(salt),
                passwordHash = hashPassword(newPassword, salt, 120_000)
            )
        )
        return newPassword
    }

    suspend fun authenticatePatient(username: String, password: String): Patient? {
        val patient = patientDao.getPatientByUsername(username.trim()) ?: return null
        val credential = credentialDao.get(patient.id) ?: return null
        val actual = hashPassword(
            password,
            Base64.getDecoder().decode(credential.salt),
            credential.iterations
        )
        if (!MessageDigest.isEqual(actual.toByteArray(), credential.passwordHash.toByteArray())) return null
        return patient.toDomain()
    }

    private fun generatePatientId(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return "SS-" + buildString { repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    private fun generatePassword(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#"
        val random = SecureRandom()
        return buildString { repeat(10) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    private fun hashPassword(password: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return Base64.getEncoder().encodeToString(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        )
    }

    suspend fun deletePatient(patient: Patient) {
        firebaseService.deletePatient(patient)
        credentialDao.deleteForPatient(patient.id)
        memoryNodeDao.deleteAllForPatient(patient.id)
        familyMemberDao.deleteAllForPatient(patient.id)
        gameSessionDao.deleteAllForPatient(patient.id)
        reminderDao.deleteAllForPatient(patient.id)
        patientDao.deletePatient(patient.toEntity())
    }

    suspend fun updateLastActive(patientId: String) {
        patientDao.updateLastActive(patientId)
    }
}
