package com.nercare.cogcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nercare.cogcare.data.local.dao.FamilyMemberDao
import com.nercare.cogcare.data.local.dao.GameSessionDao
import com.nercare.cogcare.data.local.dao.LifeMemoryNodeDao
import com.nercare.cogcare.data.local.dao.PatientDao
import com.nercare.cogcare.data.local.dao.PatientCredentialDao
import com.nercare.cogcare.data.local.dao.ReminderDao
import com.nercare.cogcare.data.local.entities.FamilyMemberEntity
import com.nercare.cogcare.data.local.entities.GameSessionEntity
import com.nercare.cogcare.data.local.entities.LifeMemoryNodeEntity
import com.nercare.cogcare.data.local.entities.PatientEntity
import com.nercare.cogcare.data.local.entities.PatientCredentialEntity
import com.nercare.cogcare.data.local.entities.ReminderEntity

@Database(
    entities = [
        // ── Existing tables ──────────────────────────────────────
        PatientEntity::class,
        PatientCredentialEntity::class,
        GameSessionEntity::class,
        ReminderEntity::class,
        // ── Life Story Vault (v2) ─────────────────────────────────
        LifeMemoryNodeEntity::class,     // Dynamic patient context graph
        FamilyMemberEntity::class        // Family & Friends Memory Gallery
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // ── Existing DAOs ──────────────────────────────────────────────
    abstract fun patientDao(): PatientDao
    abstract fun patientCredentialDao(): PatientCredentialDao
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun reminderDao(): ReminderDao

    // ── Life Story Vault DAOs (v2) ─────────────────────────────────
    abstract fun lifeMemoryNodeDao(): LifeMemoryNodeDao
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        const val DATABASE_NAME = "cogcare_db"
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patients ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN bloodGroup TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN allergies TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN primaryDoctor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN doctorContact TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN mobilityNeeds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN communicationNeeds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN dailyRoutine TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE patients ADD COLUMN sleepPattern TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_patients_username ON patients(username)")
            }
        }
    }
}
