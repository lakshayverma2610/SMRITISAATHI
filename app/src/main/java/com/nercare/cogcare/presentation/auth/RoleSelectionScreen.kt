package com.nercare.cogcare.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nercare.cogcare.presentation.theme.*

@Composable
fun RoleSelectionScreen(onCaregiver: () -> Unit, onPatient: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(BackgroundCream).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SmritiSaathi", style = MaterialTheme.typography.displaySmall, color = PrimaryGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Who is using the app today?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        RoleCard("Caregiver", "Manage patients, care details and progress", "Caregiver access", onCaregiver)
        Spacer(Modifier.height(20.dp))
        RoleCard("Patient", "Reminders, games and your Saathi companion", "Patient access", onPatient)
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
        Column(Modifier.padding(28.dp)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = TextSecondaryMuted)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(action, fontSize = 17.sp) }
        }
    }
}
