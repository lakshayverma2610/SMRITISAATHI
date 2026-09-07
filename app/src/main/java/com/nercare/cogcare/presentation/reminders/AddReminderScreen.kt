package com.nercare.cogcare.presentation.reminders

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.domain.model.Reminder
import com.nercare.cogcare.domain.model.ReminderType
import com.nercare.cogcare.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    patientId: String,
    reminderId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddReminderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val alarmManager = remember {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    var canScheduleExact by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms())
    }
    val exactAlarmAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var selectedType by remember { mutableStateOf(ReminderType.MEDICINE) }
    var title by remember { mutableStateOf("") }
    var medicineName by remember { mutableStateOf("") }
    var medicineDosage by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var existingReminder by remember { mutableStateOf<Reminder?>(null) }

    LaunchedEffect(reminderId) {
        reminderId?.let { id ->
            viewModel.loadReminder(id) { reminder ->
                existingReminder = reminder
                selectedType = reminder.type
                title = reminder.title
                medicineName = reminder.medicineName
                medicineDosage = reminder.medicineDosage
                hour = reminder.hour
                minute = reminder.minute
                selectedDays = reminder.repeatDays.toSet()
            }
        }
    }

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Box(
        modifier = Modifier.fillMaxSize()
            .background(CogCareBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Text(if (reminderId == null) "Add Reminder" else "Edit Reminder", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Type selector
            Text("Reminder Type", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderType.values().forEach { type ->
                    val isSelected = selectedType == type
                    val color = when (type) {
                        ReminderType.MEDICINE -> MedicineRed
                        ReminderType.HYDRATION -> HydrationBlue
                        ReminderType.APPOINTMENT -> AppointmentGreen
                        ReminderType.ACTIVITY -> ActivityYellow
                    }
                    Surface(
                        onClick = {
                            selectedType = type
                            if (title.isEmpty()) title = type.displayName
                        },
                        modifier = Modifier.weight(1f),
                        color = if (isSelected) color.copy(alpha = 0.25f) else CogCareSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, color) else null
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(type.emoji, fontSize = 24.sp)
                            Text(type.displayName.split(" ").first(), style = MaterialTheme.typography.labelMedium, color = if (isSelected) color else TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CogCarePrimary, focusedLabelColor = CogCarePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedType == ReminderType.MEDICINE) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("Medicine Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedicineRed, focusedLabelColor = MedicineRed, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = medicineDosage,
                    onValueChange = { medicineDosage = it },
                    label = { Text("Dosage (e.g., 1 tablet)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedicineRed, focusedLabelColor = MedicineRed, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Time picker
            Text("Time", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    // Hour picker
                    NumberPicker(value = hour, range = 0..23, onValueChange = { hour = it }, label = "HR")
                    Text(" : ", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    NumberPicker(value = minute, range = 0..59, onValueChange = { minute = it }, label = "MIN")
                }
            }
            if (!canScheduleExact) {
                TextButton(
                    onClick = {
                        exactAlarmAccess.launch(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow exact alarms for on-time reminders")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Day selector
            Text("Repeat Days", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dayNames.forEachIndexed { idx, name ->
                    val day = idx + 1
                    val isSelected = selectedDays.contains(day)
                    Surface(
                        onClick = {
                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        color = if (isSelected) CogCarePrimary.copy(alpha = 0.25f) else CogCareSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, CogCarePrimary) else null
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(name, style = MaterialTheme.typography.labelMedium, color = if (isSelected) CogCarePrimary else TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.saveReminder(
                            Reminder(
                                id = existingReminder?.id.orEmpty(),
                                patientId = patientId,
                                type = selectedType,
                                title = title,
                                description = "${selectedType.emoji} $title",
                                hour = hour,
                                minute = minute,
                                repeatDays = selectedDays.sorted(),
                                isActive = existingReminder?.isActive ?: true,
                                medicineName = medicineName,
                                medicineDosage = medicineDosage,
                                createdAt = existingReminder?.createdAt ?: System.currentTimeMillis()
                            ),
                            onSaved = onSaved
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedDays.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CogCarePrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (reminderId == null) "Save Reminder" else "Save Changes", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun NumberPicker(value: Int, range: IntRange, onValueChange: (Int) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Text("▲", style = MaterialTheme.typography.titleLarge, color = CogCarePrimary)
        }
        Text(
            value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Text("▼", style = MaterialTheme.typography.titleLarge, color = CogCarePrimary)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
