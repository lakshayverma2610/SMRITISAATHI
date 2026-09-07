package com.nercare.cogcare.data.local.dao

import androidx.room.*
import com.nercare.cogcare.data.local.entities.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    // ── Read ─────────────────────────────────────────────────────────────────

    /** Observe all family members sorted by relation priority (caregiver first, then family, then contacts). */
    @Query("SELECT * FROM family_members WHERE patientId = :patientId ORDER BY isPrimaryCaregiver DESC, createdAt ASC")
    fun observeAll(patientId: String): Flow<List<FamilyMemberEntity>>

    /** Get a single family member by ID. */
    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FamilyMemberEntity?

    /** Get the primary emergency caregiver for SOS dialing. */
    @Query("SELECT * FROM family_members WHERE patientId = :patientId AND isPrimaryCaregiver = 1 LIMIT 1")
    suspend fun getPrimaryCaregiver(patientId: String): FamilyMemberEntity?

    /** Observe only members that have a phone number (for Call / WhatsApp gallery). */
    @Query("SELECT * FROM family_members WHERE patientId = :patientId AND primaryPhone IS NOT NULL ORDER BY isPrimaryCaregiver DESC")
    fun observeCallableMembers(patientId: String): Flow<List<FamilyMemberEntity>>

    /** Observe members that have a photo (for Face Recognition Cognitive Game). */
    @Query("SELECT * FROM family_members WHERE patientId = :patientId AND mainPhotoUri IS NOT NULL")
    fun observeMembersWithPhotos(patientId: String): Flow<List<FamilyMemberEntity>>

    // ── Write ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: FamilyMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(members: List<FamilyMemberEntity>)

    /** Update the main portrait photo of a family member. */
    @Query("UPDATE family_members SET mainPhotoUri = :photoUri, updatedAt = :now WHERE id = :id")
    suspend fun updatePhoto(id: Long, photoUri: String, now: Long = System.currentTimeMillis())

    /** Update the voice note of a family member. */
    @Query("UPDATE family_members SET voiceNoteUri = :audioUri, updatedAt = :now WHERE id = :id")
    suspend fun updateVoiceNote(id: Long, audioUri: String, now: Long = System.currentTimeMillis())

    /** Mark a family member as verified by the patient. */
    @Query("UPDATE family_members SET isVerifiedByPatient = 1, updatedAt = :now WHERE id = :id")
    suspend fun markVerified(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE patientId = :patientId")
    suspend fun deleteAllForPatient(patientId: String)
}
