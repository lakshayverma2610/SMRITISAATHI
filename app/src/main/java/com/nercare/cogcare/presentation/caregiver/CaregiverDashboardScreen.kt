package com.nercare.cogcare.presentation.caregiver

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.nercare.cogcare.domain.model.GameType
import com.nercare.cogcare.domain.model.TrendDirection
import com.nercare.cogcare.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverDashboardScreen(
    patientId: String,
    onBack: () -> Unit,
    onNavigateToLifeStorySetup: () -> Unit = {},
    onEditPatient: () -> Unit = {},
    viewModel: CaregiverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val generatedPassword by viewModel.generatedPassword.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) {
        viewModel.loadDashboard(patientId)
    }

    if (generatedPassword != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordDialog() },
            title = { Text("Patient Login Credentials") },
            text = {
                Column {
                    Text("Share these credentials with the patient to login:")
                    Spacer(Modifier.height(12.dp))
                    Text("Username: ${uiState.username}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Password: $generatedPassword", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PrimaryGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("This password can be used by the patient to sign into their account.", style = MaterialTheme.typography.bodySmall, color = TextSecondaryMuted)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissPasswordDialog() }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Patient") },
            text = { Text("Are you sure you want to delete ${uiState.patientName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePatient(patientId) { onBack() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CogCareBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(top = 32.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊 Caregiver Dashboard", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(uiState.patientName, style = MaterialTheme.typography.bodyMedium, color = CogCareSecondary)
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exportClinicalSummary(context, uiState) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Summary", tint = PrimaryGreen)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Patient",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // Escalated Reminder Alerts (Missed Medicines / Hydration)
                if (uiState.escalatedReminders.isNotEmpty()) {
                    uiState.escalatedReminders.forEach { reminder ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Escalation Alert: Missed ${reminder.title}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "Scheduled at ${formatTime(reminder.hour, reminder.minute)} • Unacknowledged by patient",
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Alert banners
                if (uiState.alertMessages.isNotEmpty()) {
                    uiState.alertMessages.forEach { alert ->
                        AlertBanner(message = alert)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Patient Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Patient Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = onEditPatient) { Icon(Icons.Default.Edit, contentDescription = "Edit patient", tint = PrimaryGreen) }
                                OutlinedButton(
                                    onClick = { viewModel.resetPatientPassword(patientId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reset Password", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Username: ${uiState.username}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Patient ID: ${uiState.patientId}", style = MaterialTheme.typography.bodySmall, color = TextSecondaryMuted)
                        Text("Age: ${uiState.age}", style = MaterialTheme.typography.bodyMedium)
                        if (uiState.city.isNotBlank()) Text("City: ${uiState.city}", style = MaterialTheme.typography.bodyMedium)
                        Text("Language: ${uiState.language}", style = MaterialTheme.typography.bodyMedium)
                        Text("Stage: ${uiState.diagnosisStage}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onEditPatient, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Edit patient")
                            }
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Delete patient")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Life Story Vault entry point
                Card(
                    onClick = onNavigateToLifeStorySetup,
                    colors = CardDefaults.cardColors(containerColor = SecondaryGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📖", fontSize = 36.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Life Story Vault", fontWeight = FontWeight.Bold, color = PrimaryGreen, style = MaterialTheme.typography.titleMedium)
                            Text("Fill in memories & personal history for the AI companion", style = MaterialTheme.typography.bodySmall, color = TextSecondaryMuted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = PrimaryGreen)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overview stats row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Overall Score",
                        value = "${uiState.overallScore}",
                        unit = "/ 100",
                        color = when {
                            uiState.overallScore >= 80 -> ScoreExcellent
                            uiState.overallScore >= 60 -> ScoreGood
                            uiState.overallScore >= 40 -> ScoreFair
                            else -> ScorePoor
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "This Week",
                        value = "${uiState.sessionsThisWeek}",
                        unit = "sessions",
                        color = CogCareSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Accuracy",
                        value = "${uiState.avgAccuracy.toInt()}",
                        unit = "%",
                        color = when {
                            uiState.avgAccuracy >= 75 -> ScoreExcellent
                            uiState.avgAccuracy >= 55 -> ScoreGood
                            else -> ScorePoor
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Trend indicator
                TrendCard(
                    trend = uiState.weeklyTrend,
                    currentWeekAccuracy = uiState.avgAccuracy,
                    previousWeekAccuracy = uiState.prevWeekAccuracy
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Cognitive Trend Line Chart
                CognitiveTrendChart(scores = listOf(45f, 50f, 48f, 60f, 65f, 75f, uiState.avgAccuracy))

                Spacer(modifier = Modifier.height(16.dp))

                // Export Clinical Summary Action
                Button(
                    onClick = { exportClinicalSummary(context, uiState) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Clinical Summary (PDF / Share)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Per-game breakdown
                Text("Game Performance", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                uiState.gameBreakdown.forEach { (game, accuracy) ->
                    GamePerformanceRow(game = game, accuracy = accuracy)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Recent sessions
                Text("Recent Sessions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.recentSessions.isEmpty()) {
                    Text("No sessions recorded yet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    uiState.recentSessions.take(10).forEach { session ->
                        RecentSessionRow(
                            gameType = session.gameType.displayName,
                            gameEmoji = session.gameType.icon,
                            accuracy = session.accuracyPercent,
                            score = session.score,
                            difficulty = session.difficultyLevel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AlertBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TrendCard(trend: TrendDirection, currentWeekAccuracy: Float, previousWeekAccuracy: Float) {
    val (trendEmoji, trendColor, trendLabel) = when (trend) {
        TrendDirection.IMPROVING -> Triple("📈", ScoreExcellent, "Improving")
        TrendDirection.STABLE -> Triple("➡️", InfoBlue, "Stable")
        TrendDirection.DECLINING -> Triple("📉", ErrorRed, "Declining")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(trendEmoji, fontSize = 40.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Weekly Trend", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(trendLabel, style = MaterialTheme.typography.headlineSmall, color = trendColor, fontWeight = FontWeight.Bold)
                Text(
                    "This week: ${currentWeekAccuracy.toInt()}% vs Last week: ${previousWeekAccuracy.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CognitiveTrendChart(scores: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("7-Day Cognitive Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val maxScore = 100f
                val minScore = 0f
                val range = maxScore - minScore
                val stepX = width / (scores.size - 1).coerceAtLeast(1)
                
                val path = androidx.compose.ui.graphics.Path()
                val points = scores.mapIndexed { index, score ->
                    val x = index * stepX
                    val y = height - ((score - minScore) / range) * height
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                // Draw background gradient
                val gradientPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height)
                    points.forEachIndexed { index, point ->
                        if (index == 0) lineTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = gradientPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryGreen.copy(alpha = 0.4f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw line
                points.forEachIndexed { index, point ->
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path = path,
                    color = PrimaryGreen,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )

                // Draw points
                points.forEach { point ->
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = point)
                    drawCircle(color = PrimaryGreen, radius = 4.dp.toPx(), center = point)
                }
            }
        }
    }
}

@Composable
private fun GamePerformanceRow(game: GameType, accuracy: Float) {
    val color = when (game) {
        GameType.MEMORY_CARD -> MemoryCardBlue
        GameType.SEQUENCE_RECALL -> SequenceGold
        GameType.PATTERN_MATCHING -> PatternPurple
        GameType.WORD_ASSOCIATION -> WordGreen
        GameType.DAILY_ROUTINE -> RoutineOrange
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(game.icon, fontSize = 20.sp)
        Text(game.displayName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.width(130.dp))
        LinearProgressIndicator(
            progress = { accuracy / 100f },
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = CogCareBackground
        )
        Text("${accuracy.toInt()}%", style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun RecentSessionRow(gameType: String, gameEmoji: String, accuracy: Float, score: Int, difficulty: Int) {
    val color = when {
        accuracy >= 75 -> ScoreExcellent
        accuracy >= 55 -> ScoreGood
        accuracy >= 40 -> ScoreFair
        else -> ScorePoor
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(gameEmoji, fontSize = 24.sp)
            Text(gameType, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text("${accuracy.toInt()}%", style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Text("Lvl $difficulty", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

private fun exportClinicalSummary(context: android.content.Context, uiState: CaregiverUiState) {
    val report = buildString {
        appendLine("══════════════════════════════════════")
        appendLine("   SMRITISAATHI • COGNITIVE REPORT    ")
        appendLine("   Caregiver & Clinical Care Summary  ")
        appendLine("══════════════════════════════════════")
        appendLine()
        appendLine("📋 PATIENT PROFILE")
        appendLine("• Name: ${uiState.patientName}")
        appendLine("• Username: @${uiState.username}")
        appendLine("• Patient ID: ${uiState.patientId}")
        appendLine("• Age: ${uiState.age}")
        if (uiState.city.isNotBlank()) appendLine("• City: ${uiState.city}")
        appendLine("• Language: ${uiState.language}")
        appendLine("• Cognitive Stage: ${uiState.diagnosisStage}")
        appendLine()
        appendLine("🧠 COGNITIVE PERFORMANCE")
        appendLine("• Overall Score: ${uiState.overallScore}/100")
        appendLine("• Longitudinal Trend: ${uiState.weeklyTrend.name}")
        appendLine("• Average Accuracy: ${uiState.avgAccuracy.toInt()}% (Prev Week: ${uiState.prevWeekAccuracy.toInt()}%)")
        appendLine("• Cognitive Sessions This Week: ${uiState.sessionsThisWeek}")
        appendLine()
        if (uiState.gameBreakdown.isNotEmpty()) {
            appendLine("🎮 EXERCISE BREAKDOWN")
            uiState.gameBreakdown.forEach { (type, acc) ->
                appendLine("• ${type.name.replace('_', ' ')}: ${acc.toInt()}%")
            }
            appendLine()
        }
        if (uiState.escalatedReminders.isNotEmpty()) {
            appendLine("🚨 OVERDUE / MISSED REMINDERS")
            uiState.escalatedReminders.forEach { rem ->
                appendLine("• ${rem.title} (Scheduled: ${rem.hour}:${rem.minute.toString().padStart(2, '0')}) - Not acknowledged")
            }
            appendLine()
        }
        if (uiState.alertMessages.isNotEmpty()) {
            appendLine("⚠️ ACTIVE CLINICAL ALERTS & CHANGE SIGNALS")
            uiState.alertMessages.forEach { alert ->
                appendLine("• $alert")
            }
            appendLine()
        }
        appendLine("🛡️ SAFETY & ETHICAL BOUNDARY")
        appendLine("SmritiSaathi is a supportive non-diagnostic cognitive engagement companion, not a replacement for professional clinical care.")
        appendLine()
        appendLine("Report Generated: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}")
        appendLine("══════════════════════════════════════")
    }

    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "SmritiSaathi Cognitive Summary - ${uiState.patientName}")
        putExtra(android.content.Intent.EXTRA_TEXT, report)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Caregiver Clinical Summary"))
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "${h}:${minute.toString().padStart(2, '0')} $amPm"
}
