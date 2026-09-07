package com.nercare.cogcare.data.repository

import com.nercare.cogcare.data.local.dao.FamilyMemberDao
import com.nercare.cogcare.data.local.dao.LifeMemoryNodeDao
import com.nercare.cogcare.data.local.entities.FamilyMemberEntity
import com.nercare.cogcare.data.local.entities.LifeMemoryNodeEntity
import com.nercare.cogcare.data.local.entities.toEntity
import com.nercare.cogcare.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-entry-point repository for all Life Story, Family Gallery,
 * and Companion Tea Time data.
 *
 * All data is stored offline-first in Room. Cloud sync is handled separately
 * via WorkManager when connectivity is available.
 */
@Singleton
class LifeStoryRepository @Inject constructor(
    private val memoryNodeDao: LifeMemoryNodeDao,
    private val familyMemberDao: FamilyMemberDao
) {

    // ────────────────────────────────────────────────────────────────────────
    // Life Memory Nodes
    // ────────────────────────────────────────────────────────────────────────

    fun observeAllMemoryNodes(patientId: String): Flow<List<LifeMemoryNode>> =
        memoryNodeDao.observeAll(patientId).map { it.map(LifeMemoryNodeEntity::toDomain) }

    fun observeMemoryByDomain(patientId: String, domain: String): Flow<List<LifeMemoryNode>> =
        memoryNodeDao.observeByDomain(patientId, domain).map { it.map(LifeMemoryNodeEntity::toDomain) }

    fun observeMemoryForKey(patientId: String, domain: String, nodeKey: String): Flow<List<LifeMemoryNode>> =
        memoryNodeDao.observeAllForKey(patientId, domain, nodeKey).map { it.map(LifeMemoryNodeEntity::toDomain) }

    fun observePendingVerification(patientId: String): Flow<List<LifeMemoryNode>> =
        memoryNodeDao.observePendingVerification(patientId).map { it.map(LifeMemoryNodeEntity::toDomain) }

    /** Returns 0..100 representing what % of nodes are patient-verified. */
    fun observeLifeStoryCompletionPercent(patientId: String): Flow<Int> {
        val verified = memoryNodeDao.observeVerifiedCount(patientId)
        val total = memoryNodeDao.observeTotalCount(patientId)
        // Zipped in the ViewModel using combine(); exposed separately here for flexibility.
        return verified // ViewModel combines verified + total to compute percent.
    }

    suspend fun getMemoryNode(patientId: String, domain: String, nodeKey: String, index: Int = 0): LifeMemoryNode? =
        memoryNodeDao.getNode(patientId, domain, nodeKey, index)?.toDomain()

    /** Save a single memory node (caregiver entry or patient-answered). */
    suspend fun saveMemoryNode(node: LifeMemoryNode): Long =
        memoryNodeDao.upsertNode(node.toEntity())

    /** Bulk-save all caregiver-entered nodes from onboarding. */
    suspend fun saveCaregiverOnboardingNodes(nodes: List<LifeMemoryNode>) =
        memoryNodeDao.insertAll(nodes.map(LifeMemoryNode::toEntity))

    /**
     * Core Dual-Truth Amendment:
     * Patient confirms or corrects a caregiver-entered node.
     *
     * @param nodeId The ID of the existing node to update.
     * @param patientAnswer The patient's spoken/typed answer.
     * @param wasAmended True if patient CORRECTED the caregiver; false if patient CONFIRMED it.
     */
    suspend fun verifyOrAmendNode(nodeId: Long, patientAnswer: String, wasAmended: Boolean) {
        val status = if (wasAmended)
            VerificationStatus.DISPUTED_AMENDED.name
        else
            VerificationStatus.VERIFIED_BY_PATIENT.name

        val source = MemorySource.PATIENT_VOICE.name

        memoryNodeDao.verifyOrAmendNode(
            nodeId = nodeId,
            amendedValue = patientAnswer,
            source = source,
            verificationStatus = status
        )
    }

    suspend fun markNodeSensitiveSkip(nodeId: Long) =
        memoryNodeDao.markSensitiveSkip(nodeId)

    suspend fun attachPhotoToNode(nodeId: Long, photoUri: String) =
        memoryNodeDao.attachPhoto(nodeId, photoUri)

    suspend fun attachAudioToNode(nodeId: Long, audioUri: String) =
        memoryNodeDao.attachAudio(nodeId, audioUri)

    // ────────────────────────────────────────────────────────────────────────
    // Family Members
    // ────────────────────────────────────────────────────────────────────────

    fun observeAllFamilyMembers(patientId: String): Flow<List<FamilyMember>> =
        familyMemberDao.observeAll(patientId).map { it.map(FamilyMemberEntity::toDomain) }

    fun observeCallableMembers(patientId: String): Flow<List<FamilyMember>> =
        familyMemberDao.observeCallableMembers(patientId).map { it.map(FamilyMemberEntity::toDomain) }

    /** Returns members that have a photo — used to build the Face Recognition game deck. */
    fun observeMembersWithPhotos(patientId: String): Flow<List<FamilyMember>> =
        familyMemberDao.observeMembersWithPhotos(patientId).map { it.map(FamilyMemberEntity::toDomain) }

    suspend fun getFamilyMemberById(id: Long): FamilyMember? =
        familyMemberDao.getById(id)?.toDomain()

    suspend fun getPrimaryCaregiver(patientId: String): FamilyMember? =
        familyMemberDao.getPrimaryCaregiver(patientId)?.toDomain()

    suspend fun saveFamilyMember(member: FamilyMember): Long =
        familyMemberDao.upsertMember(member.toEntity())

    suspend fun saveFamilyMembers(members: List<FamilyMember>) =
        familyMemberDao.insertAll(members.map(FamilyMember::toEntity))

    suspend fun updateFamilyMemberPhoto(id: Long, photoUri: String) =
        familyMemberDao.updatePhoto(id, photoUri)

    suspend fun updateFamilyMemberVoiceNote(id: Long, audioUri: String) =
        familyMemberDao.updateVoiceNote(id, audioUri)

    suspend fun markFamilyMemberVerified(id: Long) =
        familyMemberDao.markVerified(id)

    suspend fun deleteFamilyMember(id: Long) {
        val member = familyMemberDao.getById(id) ?: return
        familyMemberDao.deleteMember(member)
    }
}
