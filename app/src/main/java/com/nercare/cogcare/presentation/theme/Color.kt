package com.nercare.cogcare.presentation.theme

import androidx.compose.ui.graphics.Color

// Material 3 Expressive - Soft Sage Green & Cream Palette (Elderly Friendly)
val PrimaryGreen = Color(0xFF385E4D)      // Dark forest green for prominent buttons/icons
val SecondaryGreen = Color(0xFFD5E8D4)    // Soft pastel green for large interaction cards
val TertiaryGreen = Color(0xFFE5F1E5)     // Even lighter green for backgrounds/subtle cards

val BackgroundCream = Color(0xFFF7F9F6)   // Off-white/cream for the main app background
val SurfaceWhite = Color(0xFFFFFFFF)      // Pure white for elevated cards

val TextPrimaryDark = Color(0xFF1A1C1A)   // Near black for high contrast readable text
val TextSecondaryMuted = Color(0xFF4A4E4A) // Dark gray for subtitles (maintains good contrast)

val ErrorRed = Color(0xFFBA1A1A)          // Material 3 standard error red for alerts
val WarningYellow = Color(0xFFE2A006)
val AccentMint = Color(0xFFA8E6CF)
val AccentTeal = Color(0xFF1ABC9C)
val AccentPink = Color(0xFFFF8B94)

// Old palette preserved as variables just in case they are referenced somewhere else,
// but mapped to the new theme concepts where possible so it doesn't break compilation.
val CogCarePrimary = PrimaryGreen
val CogCareSecondary = SecondaryGreen
val BackgroundGradientStart = BackgroundCream
val BackgroundGradientEnd = BackgroundCream
val SurfaceColor = SurfaceWhite
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryMuted

// Semantic colors required for existing screens
val CogCareSurfaceVariant = TertiaryGreen
val CogCarePrimaryVariant = SecondaryGreen
val TextDisabled = Color(0xFF9E9E9E)
val SuccessGreen = PrimaryGreen
val WarningAmber = WarningYellow
val InfoBlue = Color(0xFF3498DB)
val CogCareBackground = BackgroundCream

// Game colors
val MemoryCardBlue = Color(0xFF3498DB)
val SequenceGold = WarningYellow
val PatternPurple = Color(0xFF9B59B6)
val WordGreen = PrimaryGreen
val RoutineOrange = Color(0xFFE67E22)

// Reminder type colors
val MedicineRed = ErrorRed
val HydrationBlue = Color(0xFF3498DB)
val AppointmentGreen = PrimaryGreen
val ActivityYellow = WarningYellow

// Score/progress colors
val ScoreExcellent = PrimaryGreen
val ScoreGood = Color(0xFF3498DB)
val ScoreFair = WarningYellow
val ScorePoor = ErrorRed
val ErrorColor = ErrorRed
val CardBackground = SurfaceWhite
