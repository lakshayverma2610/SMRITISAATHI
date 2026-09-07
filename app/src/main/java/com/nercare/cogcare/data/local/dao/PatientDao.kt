package com.nercare.cogcare.data.local.dao

import androidx.room.*
import com.nercare.cogcare.data.local.entities.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY lastActiveAt DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE caregiverId = :caregiverId ORDER BY lastActiveAt DESC")
    fun getPatientsForCaregiver(caregiverId: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY lastActiveAt DESC")
    suspend fun getAllPatientsSync(): List<PatientEntity>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE username = :username")
    suspend fun getPatientByUsername(username: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE id = :id")
    fun observePatient(id: String): Flow<PatientEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("UPDATE patients SET lastActiveAt = :timestamp WHERE id = :id")
    suspend fun updateLastActive(id: String, timestamp: Long = System.currentTimeMillis())
}
