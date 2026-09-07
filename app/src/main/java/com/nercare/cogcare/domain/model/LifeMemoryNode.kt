package com.nercare.cogcare.domain.model

/**
 * Represents one piece of patient or caregiver-provided life-story context.
 *
 * Each node corresponds to one dynamically asked question or caregiver entry
 * (e.g., domain = "CULINARY", nodeKey = "morning_tea_type", value = "Lal Saah with ginger").
 *
 * A node can have:
 * - Multiple entries for the same key (via [memoryIndex]) to store e.g. 5 favourite songs.
 * - An attached photo ([photoUri]) or audio note ([audioUri]).
 * - A dual-truth [verificationStatus] tracking whether the patient has confirmed the data.
 */
data class LifeMemoryNode(
    val id: Long = 0,
    val patientId: String,
    val domain: String,
    val nodeKey: String,
    val questionPrompt: String,
    val value: String,
    val memoryIndex: Int = 0,
    val photoUri: String? = null,
    val audioUri: String? = null,
    val source: MemorySource = MemorySource.CAREGIVER_ENTRY,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING_CONFIRMATION,
    val dateVerified: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Who provided this memory node's data. */
enum class MemorySource {
    /** A family member or ASHA worker filled in the Caregiver Setup form. */
    CAREGIVER_ENTRY,
    /** The patient spoke the answer during a Companion Tea Time session. */
    PATIENT_VOICE,
    /** The patient typed the answer directly on screen. */
    PATIENT_TYPED,
    /** The patient filled this in alone during solo onboarding. */
    SOLO_ONBOARDING
}

/**
 * Dual-Truth Verification lifecycle.
 *
 * Every caregiver-entered node starts as [PENDING_CONFIRMATION].
 * The Companion Tea Time dialogue surfaces it to the patient for confirmation or correction.
 */
enum class VerificationStatus {
    /** Patient has not yet been asked about this node. */
    PENDING_CONFIRMATION,
    /** Patient explicitly confirmed the caregiver's entry was correct. */
    VERIFIED_BY_PATIENT,
    /** Patient corrected the caregiver's entry; [LifeMemoryNode.value] holds the patient's truth. */
    DISPUTED_AMENDED,
    /** Patient showed confusion or distress when asked; question deferred. */
    SENSITIVE_SKIP
}

/** Broad themes used to organize dynamic life-story context. */
enum class LifeStoryDomain(val displayName: String, val emoji: String) {
    SENSORY_MORNING("Morning & Daily Rituals", "🌅"),
    CULINARY("Food, Flavours & Kitchen Memories", "🍚"),
    CHILDHOOD("Childhood, Roots & Ancestral Heritage", "🏡"),
    CAREER("Career, Work & Public Service", "💼"),
    ROMANCE("Romance, Marriage & Partnership", "💍"),
    PARENTHOOD("Parenthood & Grandchildren", "👨‍👩‍👧‍👦"),
    SPIRITUAL("Faith, Devotion & Sacred Music", "🪔"),
    NER_CULTURE("North Eastern Soul & Landscapes", "🏞️"),
    EMOTIONAL_ANCHORS("Emotional Anchors, Fears & Night Safety", "🤝"),
    MEDICAL("Medical Regimen, Vitals & Safety", "💊")
}
