package com.nercare.cogcare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nercare.cogcare.domain.model.LifeMemoryNode
import com.nercare.cogcare.domain.model.MemorySource
import com.nercare.cogcare.domain.model.VerificationStatus

/**
 * Flexible graph-node entity for patient and caregiver-provided life-story context.
 *
 * Instead of 500 hardcoded columns, every memory node (e.g., "morning_tea_recipe",
 * "childhood_swimming_spot", "favourite_bhajan_singer") is stored as a named key-value
 * pair with rich metadata for verification and multi-media support.
 *
 * Schema decisions:
 * - [domain] groups related questions (e.g., "CULINARY", "CHILDHOOD", "SPIRITUAL").
 * - [nodeKey] uniquely identifies a specific question within a domain.
 * - [value] stores the free-text answer (spoken or typed).
 * - [source] tracks whether data came from a caregiver or directly from the patient.
 * - [verificationStatus] supports the Dual-Truth self-amending protocol:
 *     PENDING -> VERIFIED_BY_PATIENT (confirmed) or DISPUTED_AMENDED (corrected).
 * - Multiple entries with the same nodeKey but different [memoryIndex] allow
 *   unlimited memory additions (e.g., Memory 1, Memory 2 for "Favourite Songs").
 */
@Entity(
    tableName = "life_memory_nodes",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["patientId", "domain", "nodeKey", "memoryIndex"], unique = true)
    ]
)
data class LifeMemoryNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** FK to the patient this memory belongs to. */
    val patientId: String,

    /**
     * Broad thematic domain used to organize dynamic context.
     * e.g., SENSORY_MORNING, CULINARY, CHILDHOOD, CAREER, ROMANCE,
     * PARENTHOOD, SPIRITUAL, NER_CULTURE, EMOTIONAL_ANCHORS, MEDICAL
     */
    val domain: String,

    /**
     * Unique key identifying the specific question within a domain.
     * e.g., "morning_tea_type", "first_school_name", "favourite_fish_dish"
     */
    val nodeKey: String,

    /**
     * The human-readable question prompt shown / spoken to the patient or caregiver.
     * e.g., "How do you like your morning tea?"
     */
    val questionPrompt: String,

    /**
     * The free-text answer captured via voice transcription or typed input.
     * e.g., "Lal Saah, ginger, one spoon sugar, very hot"
     */
    val value: String,

    /**
     * For nodes that support multiple memories (e.g., multiple songs, multiple friends),
     * index starts at 0. Default 0 for single-value nodes.
     */
    val memoryIndex: Int = 0,

    /**
     * Optional local file URI for a photo associated with this memory.
     * e.g., photo of the patient's spectacles, medicine strip, family member.
     */
    val photoUri: String? = null,

    /**
     * Optional local file URI for the patient's or family member's recorded voice note
     * for this memory node.
     */
    val audioUri: String? = null,

    /**
     * Who provided this data.
     * Values: "CAREGIVER_ENTRY", "PATIENT_VOICE", "PATIENT_TYPED", "SOLO_ONBOARDING"
     */
    val source: String = MemorySource.CAREGIVER_ENTRY.name,

    /**
     * Dual-Truth verification status.
     * PENDING_CONFIRMATION -> patient has not yet been asked.
     * VERIFIED_BY_PATIENT  -> patient confirmed the caregiver's entry.
     * DISPUTED_AMENDED     -> patient corrected the caregiver; value is the amended truth.
     * SENSITIVE_SKIP       -> patient was confused or distressed; skip for now.
     */
    val verificationStatus: String = VerificationStatus.PENDING_CONFIRMATION.name,

    /** Timestamp of when the patient last verified or amended this node. */
    val dateVerified: Long? = null,

    /** Timestamp of when the node was first created. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp of last update to value, photo, or audio. */
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = LifeMemoryNode(
        id = id,
        patientId = patientId,
        domain = domain,
        nodeKey = nodeKey,
        questionPrompt = questionPrompt,
        value = value,
        memoryIndex = memoryIndex,
        photoUri = photoUri,
        audioUri = audioUri,
        source = MemorySource.valueOf(source),
        verificationStatus = VerificationStatus.valueOf(verificationStatus),
        dateVerified = dateVerified,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LifeMemoryNode.toEntity() = LifeMemoryNodeEntity(
    id = id,
    patientId = patientId,
    domain = domain,
    nodeKey = nodeKey,
    questionPrompt = questionPrompt,
    value = value,
    memoryIndex = memoryIndex,
    photoUri = photoUri,
    audioUri = audioUri,
    source = source.name,
    verificationStatus = verificationStatus.name,
    dateVerified = dateVerified,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)
