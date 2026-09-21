package com.nercare.cogcare.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.nercare.cogcare.data.local.AppDatabase
import com.nercare.cogcare.data.local.dao.*
import net.sqlcipher.database.SupportFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val passphrase = net.sqlcipher.database.SQLiteDatabase.getBytes("super_secret_key_123".toCharArray())
        val supportFactory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .openHelperFactory(supportFactory)
        .addMigrations(AppDatabase.MIGRATION_6_7)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun providePatientDao(db: AppDatabase): PatientDao = db.patientDao()

    @Provides
    fun providePatientCredentialDao(db: AppDatabase): PatientCredentialDao = db.patientCredentialDao()

    @Provides
    fun provideGameSessionDao(db: AppDatabase): GameSessionDao = db.gameSessionDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideLifeMemoryNodeDao(db: AppDatabase): LifeMemoryNodeDao = db.lifeMemoryNodeDao()

    @Provides
    fun provideFamilyMemberDao(db: AppDatabase): FamilyMemberDao = db.familyMemberDao()

    @Provides
    fun provideGameContentDao(db: AppDatabase): GameContentDao = db.gameContentDao()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        // Enable offline persistence with 100MB cache
        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {
                setSizeBytes(100 * 1024 * 1024L) // 100 MB
            })
        }
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder().build()
    }

}
