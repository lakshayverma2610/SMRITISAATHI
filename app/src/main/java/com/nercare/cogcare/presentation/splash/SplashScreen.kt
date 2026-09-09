package com.nercare.cogcare.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nercare.cogcare.presentation.theme.CogCareBackground
import com.nercare.cogcare.presentation.theme.PrimaryGreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.CaregiverAuthRepository
import com.nercare.cogcare.data.repository.SessionRepository
import com.nercare.cogcare.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val caregiverAuth: CaregiverAuthRepository
) : ViewModel() {
    private val _route = MutableStateFlow<String?>(null)
    val route = _route.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.getSession()
            _route.value = when {
                session?.role == "patient" && !session.patientId.isNullOrBlank() -> Screen.Home.createRoute(session.patientId)
                session?.role == "caregiver" && caregiverAuth.currentCaregiverId != null -> Screen.PatientSelection.route
                caregiverAuth.currentCaregiverId != null -> {
                    sessionRepository.saveCaregiverSession()
                    Screen.PatientSelection.route
                }
                else -> {
                    sessionRepository.clear()
                    Screen.RoleSelection.route
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onNavigate: (String) -> Unit, viewModel: SplashViewModel = hiltViewModel()) {
    val route = viewModel.route.collectAsState().value
    LaunchedEffect(route) { route?.let(onNavigate) }
    Box(Modifier.fillMaxSize().background(CogCareBackground), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryGreen)
    }
}
