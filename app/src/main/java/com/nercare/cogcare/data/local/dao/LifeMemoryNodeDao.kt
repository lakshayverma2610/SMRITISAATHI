package com.nercare.cogcare.data.local.dao

import androidx.room.*
import com.nercare.cogcare.data.local.entities.LifeMemoryNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeMemoryNodeDao {

    // ── Read ─────────────────────────────────────────────────────────────────

    /** Observe all memory nodes for a patient, ordered newest-first. */
    @Query("SELECT * FROM life_memory_nodes WHERE patientId = :patientId ORDER BY updatedAt DESC")
    fun observeAll(patientId: String): Flow<List<LifeMemoryNodeEntity>>

    /** Observe nodes filtered by a specific domain (e.g. "CULINARY"). */
    @Query("SELECT * FROM life_memory_nodes WHERE patientId = :patientId AND domain = :domain ORDER BY nodeKey, memoryIndex")
    fun observeByDomain(patientId: String, domain: String): Flow<List<LifeMemoryNodeEntity>>

    /** Get a specific node by domain + key + index (for single-value nodes index = 0). */
    @Query("SELECT * FROM life_memory_nodes WHERE patientId = :patientId AND domain = :domain AND nodeKey = :nodeKey AND memoryIndex = :memoryIndex LIMIT 1")
    suspend fun getNode(patientId: String, domain: String, nodeKey: String, memoryIndex: Int = 0): LifeMemoryNodeEntity?

    /** Get all entries for a multi-value node key (e.g., all favourite songs). */
    @Query("SELECT * FROM life_memory_nodes WHERE patientId = :patientId AND domain = :domain AND nodeKey = :nodeKey ORDER BY memoryIndex")
    fun observeAllForKey(patientId: String, domain: String, nodeKey: String): Flow<List<LifeMemoryNodeEntity>>

    /** Fetch nodes that are still PENDING_CONFIRMATION (not yet verified by patient). */
    @Query("SELECT * FROM life_memory_nodes WHERE patientId = :patientId AND verificationStatus = 'PENDING_CONFIRMATION' ORDER BY createdAt ASC")
    fun observePendingVerification(patientId: String): Flow<List<LifeMemoryNodeEntity>>

    /** Count verified nodes for a patient — shown as a "Life Story completion %" in the UI. */
    @Query("SELECT COUNT(*) FROM life_memory_nodes WHERE patientId = :patientId AND verificationStatus = 'VERIFIED_BY_PATIENT'")
    fun observeVerifiedCount(patientId: String): Flow<Int>

    /** Count all nodes regardless of status. */
    @Query("SELECT COUNT(*) FROM life_memory_nodes WHERE patientId = :patientId")
    fun observeTotalCount(patientId: String): Flow<Int>

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Insert or replace a node (used for both new entries and amendments). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNode(node: LifeMemoryNodeEntity): Long

    /** Bulk insert from caregiver onboarding (all domains at once). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(nodes: List<LifeMemoryNodeEntity>)

    /**
     * Patient verification: confirm or amend a caregiver entry.
     * Updates value + source + verificationStatus + dateVerified atomically.
     */
    @Query("""
        UPDATE life_memory_nodes
        SET value = :amendedValue,
            source = :source,
            verificationStatus = :verificationStatus,
            dateVerified = :now,
            updatedAt = :now
        WHERE id = :nodeId
    """)
    suspend fun verifyOrAmendNode(
        nodeId: Long,
        amendedValue: String,
        source: String,
        verificationStatus: String,
        now: Long = System.currentTimeMillis()
    )

    /** Mark a node as SENSITIVE_SKIP (patient distressed when asked). */
    @Query("UPDATE life_memory_nodes SET verificationStatus = 'SENSITIVE_SKIP', updatedAt = :now WHERE id = :nodeId")
    suspend fun markSensitiveSkip(nodeId: Long, now: Long = System.currentTimeMillis())

    /** Attach a photo URI to an existing node. */
    @Query("UPDATE life_memory_nodes SET photoUri = :photoUri, updatedAt = :now WHERE id = :nodeId")
    suspend fun attachPhoto(nodeId: Long, photoUri: String, now: Long = System.currentTimeMillis())

    /** Attach an audio note URI to an existing node. */
    @Query("UPDATE life_memory_nodes SET audioUri = :audioUri, updatedAt = :now WHERE id = :nodeId")
    suspend fun attachAudio(nodeId: Long, audioUri: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteNode(node: LifeMemoryNodeEntity)

    /** Wipe all memory nodes for a patient (used on full data reset). */
    @Query("DELETE FROM life_memory_nodes WHERE patientId = :patientId")
    suspend fun deleteAllForPatient(patientId: String)
}
