package com.nercare.cogcare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_credentials")
data class PatientCredentialEntity(
    @PrimaryKey val patientId: String,
    val salt: String,
    val passwordHash: String,
    val iterations: Int = 120_000
)
