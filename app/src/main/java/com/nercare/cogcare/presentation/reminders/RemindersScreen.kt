package com.nercare.cogcare.presentation.reminders

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.domain.model.Reminder
import com.nercare.cogcare.domain.model.ReminderType
import com.nercare.cogcare.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    patientId: String,
    onAddReminder: (String) -> Unit,
    onEditReminder: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(patientId) {
        viewModel.loadReminders(patientId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CogCareBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your daily schedule",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondaryMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Summary chips
            ReminderTypeSummary(reminders = uiState.reminders)

            Spacer(modifier = Modifier.height(8.dp))

            // Reminder list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                if (uiState.reminders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(androidx.compose.material.icons.Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No reminders yet", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                            Text("Add your first reminder below", style = MaterialTheme.typography.bodyMedium, color = TextDisabled)
                        }
                    }
                } else {
                    ReminderType.values().forEach { type ->
                        val typeReminders = uiState.reminders.filter { it.type == type }
                        if (typeReminders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                type.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = reminderColor(type)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            typeReminders.forEach { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onToggle = { viewModel.toggleReminder(reminder) },
                                    onEdit = { onEditReminder(patientId, reminder.id) },
                                    onDelete = { viewModel.deleteReminder(reminder.id, patientId) },
                                    onAcknowledge = { viewModel.acknowledgeReminder(reminder) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { onAddReminder(patientId) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = CogCarePrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Reminder")
        }
    }
}

@Composable
private fun ReminderTypeSummary(reminders: List<Reminder>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReminderType.values().forEach { type ->
            val count = reminders.count { it.type == type && it.isActive }
            val color = reminderColor(type)
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(type.displayName.take(1), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
                    Text("$count", style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAcknowledge: () -> Unit
) {
    val color = reminderColor(reminder.type)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(reminder.type.displayName.take(1), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (reminder.medicineName.isNotEmpty()) {
                        Text(reminder.medicineName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Text(
                        "${formatTime(reminder.hour, reminder.minute)} • ${daysLabel(reminder.repeatDays)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Switch(
                        checked = reminder.isActive,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = color)
                    )
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = color, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Acknowledgement / Taken action
            if (reminder.isAcknowledged) {
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onAcknowledge() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Completed / Taken today (tap to undo)", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark as Taken / Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun reminderColor(type: ReminderType) = when (type) {
    ReminderType.MEDICINE -> MedicineRed
    ReminderType.HYDRATION -> HydrationBlue
    ReminderType.APPOINTMENT -> AppointmentGreen
    ReminderType.ACTIVITY -> ActivityYellow
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "${h}:${minute.toString().padStart(2, '0')} $amPm"
}

private fun daysLabel(days: List<Int>): String {
    if (days.size == 7) return "Every day"
    val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    return days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
}
