package com.nercare.cogcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nercare.cogcare.data.local.dao.*
import com.nercare.cogcare.data.local.entities.*

@Database(
    entities = [
        // ── Existing tables ──────────────────────────────────────
        PatientEntity::class,
        PatientCredentialEntity::class,
        GameSessionEntity::class,
        ReminderEntity::class,
        // ── Life Story Vault (v2) ─────────────────────────────────
        LifeMemoryNodeEntity::class,     // Dynamic patient context graph
        FamilyMemberEntity::class,       // Family & Friends Memory Gallery
        GameContentEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun patientCredentialDao(): PatientCredentialDao
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun reminderDao(): ReminderDao

    // ── Life Story Vault DAOs (v2) ─────────────────────────────────
    abstract fun lifeMemoryNodeDao(): LifeMemoryNodeDao
    abstract fun familyMemberDao(): FamilyMemberDao

    // ── Phase 6 ───────────────────────────────────────────────────
    abstract fun gameContentDao(): GameContentDao

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
