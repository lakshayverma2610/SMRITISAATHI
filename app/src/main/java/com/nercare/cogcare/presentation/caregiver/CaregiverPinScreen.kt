package com.nercare.cogcare.presentation.caregiver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nercare.cogcare.presentation.theme.*

import kotlinx.coroutines.launch

@Composable
fun CaregiverPinScreen(
    patientId: String,
    onPinSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val correctPin = "1234"

    // Shake animation state
    val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..2) {
                shakeOffset.animateTo(15f, animationSpec = androidx.compose.animation.core.tween(50))
                shakeOffset.animateTo(-15f, animationSpec = androidx.compose.animation.core.tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(50))
        }
    }

    fun onKeyPressed(digit: String) {
        if (pin.length < 4) {
            isError = false
            val newPin = pin + digit
            pin = newPin
            if (newPin.length == 4) {
                if (newPin == correctPin) {
                    onPinSuccess()
                } else {
                    isError = true
                    triggerShake()
                    pin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            isError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lock Icon Badge
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = SecondaryGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Caregiver Security PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your 4-digit PIN to access clinical metrics and care settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 4 PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(x = shakeOffset.value.dp)
        ) {
            for (i in 0 until 4) {
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) ErrorRed
                            else if (filled) PrimaryGreen
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isError) ErrorRed else PrimaryGreen,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Error message
        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "Incorrect PIN. Please try again.",
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Numeric Keypad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            keyRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        when (key) {
                            "DEL" -> {
                                KeypadButton(
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = { onBackspace() }
                                )
                            }
                            "C" -> {
                                KeypadButton(
                                    content = {
                                        Text(
                                            text = "C",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondaryMuted
                                        )
                                    },
                                    onClick = {
                                        pin = ""
                                        isError = false
                                    }
                                )
                            }
                            else -> {
                                KeypadButton(
                                    content = {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    },
                                    onClick = { onKeyPressed(key) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick demo hint
        Text(
            text = "Demo PIN: 1234",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryMuted,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun KeypadButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGreen)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
