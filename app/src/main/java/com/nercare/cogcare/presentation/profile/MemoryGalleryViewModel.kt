package com.nercare.cogcare.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.LifeMemoryNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryGalleryUiState(
    val memories: List<LifeMemoryNode> = emptyList(),
    val familyMembers: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MemoryGalleryViewModel @Inject constructor(
    private val lifeStoryRepository: LifeStoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MemoryGalleryUiState())
    val uiState: StateFlow<MemoryGalleryUiState> = _uiState.asStateFlow()
    private var loadedPatientId: String? = null

    fun load(patientId: String) {
        if (loadedPatientId == patientId) return
        loadedPatientId = patientId
        viewModelScope.launch {
            combine(
                lifeStoryRepository.observeAllMemoryNodes(patientId),
                lifeStoryRepository.observeAllFamilyMembers(patientId)
            ) { memories, family -> memories to family }
                .collect { (memories, family) ->
                    _uiState.update {
                        it.copy(memories = memories, familyMembers = family, isLoading = false)
                    }
                }
        }
    }

    fun addFamilyMember(
        fullName: String,
        relationLabel: String,
        photoUri: String? = null
    ) {
        val patientId = loadedPatientId ?: return
        viewModelScope.launch {
            val relation = com.nercare.cogcare.domain.model.FamilyRelation.entries
                .firstOrNull { it.displayLabel.equals(relationLabel.trim(), ignoreCase = true) }
                ?: com.nercare.cogcare.domain.model.FamilyRelation.OTHER
            val member = FamilyMember(
                patientId = patientId,
                fullName = fullName.trim(),
                relation = relation,
                mainPhotoUri = photoUri
            )
            lifeStoryRepository.saveFamilyMember(member)
        }
    }
}
