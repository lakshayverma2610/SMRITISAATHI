package com.nercare.cogcare.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nercare.cogcare.presentation.theme.CogCareBackground
import com.nercare.cogcare.presentation.theme.PrimaryGreen

@Composable
fun SplashScreen(onNavigate: (String) -> Unit) {
    LaunchedEffect(Unit) { onNavigate("role_selection") }
    Box(Modifier.fillMaxSize().background(CogCareBackground), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryGreen)
    }
}
