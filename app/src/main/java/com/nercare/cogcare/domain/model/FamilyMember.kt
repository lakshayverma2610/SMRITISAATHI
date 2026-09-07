package com.nercare.cogcare.domain.model

/**
 * Represents a person in the patient's inner circle — family, friends, or key contacts.
 *
 * Used for:
 * - Family & Friends Memory Gallery (photo grid + detail profile page).
 * - Face & Name Recognition Cognitive Games.
 * - One-touch call and SOS dialing.
 * - Personalised grounding scripts ("Your son [fullName] is in the next room").
 */
data class FamilyMember(
    val id: Long = 0,
    val patientId: String,
    val fullName: String,
    val petNameByPatient: String? = null,
    val relation: FamilyRelation,
    val currentLocation: String? = null,
    val occupation: String? = null,
    val primaryPhone: String? = null,
    val whatsappPhone: String? = null,
    val mainPhotoUri: String? = null,
    val voiceNoteUri: String? = null,
    val favouriteSharedMemory: String? = null,
    val isPrimaryCaregiver: Boolean = false,
    val isVerifiedByPatient: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** All possible relation types that appear in the Family Gallery. */
enum class FamilyRelation(val displayLabel: String) {
    SPOUSE("Spouse"),
    ELDER_SON("Elder Son"),
    YOUNGER_SON("Younger Son"),
    ELDER_DAUGHTER("Elder Daughter"),
    YOUNGER_DAUGHTER("Younger Daughter"),
    SON_IN_LAW("Son-in-Law"),
    DAUGHTER_IN_LAW("Daughter-in-Law"),
    GRANDCHILD("Grandchild"),
    ELDER_BROTHER("Elder Brother"),
    YOUNGER_BROTHER("Younger Brother"),
    ELDER_SISTER("Elder Sister"),
    YOUNGER_SISTER("Younger Sister"),
    CLOSE_FRIEND("Close Friend"),
    NEIGHBOUR("Neighbour"),
    DOCTOR("Doctor"),
    ASHA_WORKER("ASHA / Health Worker"),
    OTHER("Other")
}
