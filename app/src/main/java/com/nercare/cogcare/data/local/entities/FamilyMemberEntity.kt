package com.nercare.cogcare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.FamilyRelation

/**
 * Stores each person in the patient's inner circle:
 * spouse, sons, daughters, grandchildren, siblings, friends, doctor, ASHA worker.
 *
 * Each record drives:
 * - The Family & Friends Memory Gallery UI.
 * - Face Recognition Cognitive Games (using [mainPhotoUri]).
 * - One-Touch Call/SOS triggers (using [primaryPhone]).
 * - Personalized grounding scripts (using [petNameByPatient]).
 */
@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["patientId"])]
)
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val patientId: String,

    /** Full name (e.g., "Bikash Kumar Sharma"). */
    val fullName: String,

    /**
     * What the patient calls this person affectionately.
     * e.g., "Guddu", "Mamoni", "Babai", "Noni"
     */
    val petNameByPatient: String? = null,

    /**
     * Relation category used for grouping in the Gallery.
     * Values: SPOUSE, ELDER_SON, YOUNGER_SON, ELDER_DAUGHTER, YOUNGER_DAUGHTER,
     * GRANDCHILD, SIBLING, CLOSE_FRIEND, DOCTOR, ASHA_WORKER, OTHER
     */
    val relation: String,

    /** City / town where this person currently lives. */
    val currentLocation: String? = null,

    /** Their occupation / designation. */
    val occupation: String? = null,

    /** Primary mobile number for one-touch dialing. */
    val primaryPhone: String? = null,

    /** WhatsApp number if different from primary phone. */
    val whatsappPhone: String? = null,

    /** Local URI of the main portrait photo (shown as the Gallery card). */
    val mainPhotoUri: String? = null,

    /**
     * Local URI for a short real voice note recorded by this person.
     * e.g., "Deuta, it's Bikash. All is well, I'll visit Sunday."
     * Played when the patient taps the speaker icon on this person's profile.
     */
    val voiceNoteUri: String? = null,

    /**
     * A favourite shared memory the caregiver or patient described.
     * Used as the caption in the Memory Gallery and as a game question hint.
     */
    val favouriteSharedMemory: String? = null,

    /** Is this person the primary emergency caregiver? */
    val isPrimaryCaregiver: Boolean = false,

    /** Is this person's entry verified by the patient? */
    val isVerifiedByPatient: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = FamilyMember(
        id = id,
        patientId = patientId,
        fullName = fullName,
        petNameByPatient = petNameByPatient,
        relation = FamilyRelation.valueOf(relation),
        currentLocation = currentLocation,
        occupation = occupation,
        primaryPhone = primaryPhone,
        whatsappPhone = whatsappPhone,
        mainPhotoUri = mainPhotoUri,
        voiceNoteUri = voiceNoteUri,
        favouriteSharedMemory = favouriteSharedMemory,
        isPrimaryCaregiver = isPrimaryCaregiver,
        isVerifiedByPatient = isVerifiedByPatient,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun FamilyMember.toEntity() = FamilyMemberEntity(
    id = id,
    patientId = patientId,
    fullName = fullName,
    petNameByPatient = petNameByPatient,
    relation = relation.name,
    currentLocation = currentLocation,
    occupation = occupation,
    primaryPhone = primaryPhone,
    whatsappPhone = whatsappPhone,
    mainPhotoUri = mainPhotoUri,
    voiceNoteUri = voiceNoteUri,
    favouriteSharedMemory = favouriteSharedMemory,
    isPrimaryCaregiver = isPrimaryCaregiver,
    isVerifiedByPatient = isVerifiedByPatient,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)
