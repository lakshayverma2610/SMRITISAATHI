package com.nercare.cogcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nercare.cogcare.data.local.entities.PatientCredentialEntity

@Dao
interface PatientCredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(credential: PatientCredentialEntity)

    @Query("SELECT * FROM patient_credentials WHERE patientId = :patientId LIMIT 1")
    suspend fun get(patientId: String): PatientCredentialEntity?

    @Query("DELETE FROM patient_credentials WHERE patientId = :patientId")
    suspend fun deleteForPatient(patientId: String)
}
