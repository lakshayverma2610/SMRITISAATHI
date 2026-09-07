package com.nercare.cogcare.presentation.games.generic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalizedGameViewModel @Inject constructor(
    private val patients: PatientRepository,
    private val lifeStory: LifeStoryRepository
) : ViewModel() {
    private val _questions = MutableStateFlow<List<GameQuestion>>(emptyList())
    val questions = _questions.asStateFlow()

    fun load(patientId: String, gameId: String) = viewModelScope.launch {
        val patient = patients.getPatientById(patientId)
        val memories = lifeStory.observeAllMemoryNodes(patientId).first()
        val personal = buildList {
            if (gameId == "life_story_recall" || gameId == "familiar_music" || gameId == "category_word") {
                memories.filter { it.value.isNotBlank() && !it.domain.equals("MEDICAL", true) }.take(4).forEach { memory ->
                    val correct = memory.value.take(60)
                    val alternatives = listOf("Something different", "I am not sure today")
                    add(GameQuestion("Which answer belongs in your life story: ${memory.questionPrompt.removePrefix("Caregiver onboarding:")}?", listOf(correct) + alternatives, 0, "Yes, $correct is part of your story."))
                }
                if (patient != null && patient.favoriteMusic.isNotBlank() && none { it.options.first() == patient.favoriteMusic }) {
                    add(GameQuestion("Which music was chosen for you?", listOf(patient.favoriteMusic, "Something different", "No preference"), 0, "Your profile says you enjoy ${patient.favoriteMusic}."))
                }
            }
        }
        _questions.value = (personal + getQuestionsForGame(gameId)).take(5)
    }
}
