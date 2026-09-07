package com.nercare.cogcare.presentation.caregiver.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LifeStorySetupUiState(val memories: List<LifeMemoryNode> = emptyList(), val saved: Boolean = false)
@HiltViewModel
class LifeStorySetupViewModel @Inject constructor(private val repository: LifeStoryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LifeStorySetupUiState())
    val uiState = _uiState.asStateFlow()
    private var patientId = ""
    fun load(id: String) {
        if (patientId == id) return
        patientId = id
        viewModelScope.launch { repository.observeAllMemoryNodes(id).collect { _uiState.value = LifeStorySetupUiState(it) } }
    }
    fun save(label: String, value: String, domain: String) = viewModelScope.launch {
        if (value.isBlank()) return@launch
        repository.saveMemoryNode(LifeMemoryNode(patientId = patientId, domain = domain.ifBlank { "PERSONAL" }.uppercase(), nodeKey = "caregiver_${System.currentTimeMillis()}", questionPrompt = label.ifBlank { "Something important to know" }, value = value.trim(), source = MemorySource.CAREGIVER_ENTRY))
        _uiState.update { it.copy(saved = true) }
    }
}
