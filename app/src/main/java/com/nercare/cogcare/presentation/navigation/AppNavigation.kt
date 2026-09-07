package com.nercare.cogcare.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import com.nercare.cogcare.presentation.caregiver.CaregiverDashboardScreen
import com.nercare.cogcare.presentation.caregiver.CaregiverPinScreen
import com.nercare.cogcare.presentation.caregiver.setup.LifeStorySetupScreen
import com.nercare.cogcare.presentation.voice.CompanionVoiceScreen
import com.nercare.cogcare.presentation.games.GameHubScreen
import com.nercare.cogcare.presentation.games.generic.GenericCognitiveGameScreen
import com.nercare.cogcare.presentation.games.memory.MemoryCardGameScreen
import com.nercare.cogcare.presentation.games.pattern.PatternMatchingGameScreen
import com.nercare.cogcare.presentation.games.routine.DailyRoutineGameScreen
import com.nercare.cogcare.presentation.games.sequence.SequenceRecallGameScreen
import com.nercare.cogcare.presentation.games.wordassoc.WordAssociationGameScreen
import com.nercare.cogcare.presentation.home.HomeScreen
import com.nercare.cogcare.presentation.onboarding.OnboardingScreen
import com.nercare.cogcare.presentation.auth.CaregiverAuthScreen
import com.nercare.cogcare.presentation.auth.PatientLoginScreen
import com.nercare.cogcare.presentation.auth.PatientSelectionScreen
import com.nercare.cogcare.presentation.auth.RoleSelectionScreen
import com.nercare.cogcare.presentation.splash.SplashScreen
import com.nercare.cogcare.presentation.profile.MemoryGalleryScreen
import com.nercare.cogcare.presentation.reminders.RemindersScreen
import com.nercare.cogcare.presentation.reminders.AddReminderScreen
import com.nercare.cogcare.presentation.matching.PatientMatchingScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController, 
        startDestination = startDestination,
        modifier = modifier
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onCaregiver = { navController.navigate(Screen.CaregiverAuth.route) },
                onPatient = { navController.navigate(Screen.PatientLogin.route) }
            )
        }

        composable(Screen.CaregiverAuth.route) {
            CaregiverAuthScreen(
                onSuccess = { navController.navigate(Screen.PatientSelection.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PatientSelection.route) {
            PatientSelectionScreen(
                onPatient = { navController.navigate(Screen.CaregiverDashboard.createRoute(it)) },
                onAdd = { navController.navigate(Screen.Onboarding.createRoute(it)) },
                onSignedOut = { navController.navigate(Screen.RoleSelection.route) { popUpTo(0) } }
            )
        }

        composable(Screen.PatientLogin.route) {
            PatientLoginScreen(
                onSuccess = { navController.navigate(Screen.Home.createRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Onboarding.route,
            arguments = listOf(navArgument("caregiverId") { type = NavType.StringType })
        ) { backStack ->
            OnboardingScreen(
                caregiverId = backStack.arguments?.getString("caregiverId") ?: "",
                onComplete = { patientId ->
                    navController.navigate(Screen.CaregiverDashboard.createRoute(patientId)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            val patientId = backStack.arguments?.getString("patientId") ?: ""
            HomeScreen(
                patientId = patientId,
                onNavigateToGames = { navController.navigate(Screen.GameHub.createRoute(patientId)) },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.createRoute(patientId)) },
                onNavigateToCaregiverDashboard = { navController.navigate(Screen.CaregiverAuth.route) },
                onNavigateToProfile = { navController.navigate(Screen.PatientProfile.createRoute(patientId)) },
                onNavigateToMatching = { navController.navigate(Screen.PatientMatching.createRoute(patientId)) },
                onNavigateToVoice = { navController.navigate(Screen.CompanionVoice.createRoute(patientId)) },
                onNavigateToLifeStory = { navController.navigate(Screen.CompanionVoice.createRoute(patientId, mode = "lifestory")) },
                onNavigateToFamilyCircle = { navController.navigate(Screen.FamilyCircle.createRoute(patientId)) }
            )
        }

        composable(
            route = Screen.GameHub.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            val patientId = backStack.arguments?.getString("patientId") ?: ""
            GameHubScreen(
                patientId = patientId,
                onNavigateToGame = { gameType, difficulty ->
                    when (gameType) {
                        "MEMORY_CARD" -> navController.navigate(Screen.MemoryCardGame.createRoute(patientId, difficulty))
                        "SEQUENCE_RECALL" -> navController.navigate(Screen.SequenceGame.createRoute(patientId, difficulty))
                        "PATTERN_MATCHING" -> navController.navigate(Screen.PatternGame.createRoute(patientId, difficulty))
                        "WORD_ASSOCIATION" -> navController.navigate(Screen.WordAssocGame.createRoute(patientId, difficulty))
                        "DAILY_ROUTINE" -> navController.navigate(Screen.RoutineGame.createRoute(patientId, difficulty))
                    }
                },
                onNavigateToGenericGame = { gameId, gameTitle, domain ->
                    navController.navigate(Screen.GenericCognitiveGame.createRoute(patientId, gameId, gameTitle, domain))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GenericCognitiveGame.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("gameId") { type = NavType.StringType },
                navArgument("gameTitle") { type = NavType.StringType },
                navArgument("domain") { type = NavType.StringType }
            )
        ) { backStack ->
            val patientId = backStack.arguments?.getString("patientId") ?: ""
            val gameId = backStack.arguments?.getString("gameId") ?: ""
            val gameTitle = java.net.URLDecoder.decode(backStack.arguments?.getString("gameTitle") ?: "Cognitive Exercise", "UTF-8")
            val domain = java.net.URLDecoder.decode(backStack.arguments?.getString("domain") ?: "Cognitive", "UTF-8")
            GenericCognitiveGameScreen(
                patientId = patientId,
                gameId = gameId,
                gameTitle = gameTitle,
                domain = domain,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MemoryCardGame.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { backStack ->
            MemoryCardGameScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                difficulty = backStack.arguments?.getInt("difficulty") ?: 2,
                onGameComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SequenceGame.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { backStack ->
            SequenceRecallGameScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                difficulty = backStack.arguments?.getInt("difficulty") ?: 2,
                onGameComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PatternGame.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { backStack ->
            PatternMatchingGameScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                difficulty = backStack.arguments?.getInt("difficulty") ?: 2,
                onGameComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WordAssocGame.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { backStack ->
            WordAssociationGameScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                difficulty = backStack.arguments?.getInt("difficulty") ?: 2,
                onGameComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RoutineGame.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { backStack ->
            DailyRoutineGameScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                difficulty = backStack.arguments?.getInt("difficulty") ?: 2,
                onGameComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Reminders.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            RemindersScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                onAddReminder = { patientId ->
                    navController.navigate(Screen.AddReminder.createRoute(patientId))
                },
                onEditReminder = { patientId, reminderId ->
                    navController.navigate(Screen.EditReminder.createRoute(patientId, reminderId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddReminder.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            AddReminderScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditReminder.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("reminderId") { type = NavType.StringType }
            )
        ) { backStack ->
            AddReminderScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                reminderId = backStack.arguments?.getString("reminderId"),
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CaregiverPin.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            val patientId = backStack.arguments?.getString("patientId") ?: ""
            CaregiverPinScreen(
                patientId = patientId,
                onPinSuccess = {
                    navController.navigate(Screen.CaregiverDashboard.createRoute(patientId)) {
                        popUpTo(Screen.CaregiverPin.createRoute(patientId)) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CaregiverDashboard.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            val patientId = backStack.arguments?.getString("patientId") ?: ""
            CaregiverDashboardScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() },
                onNavigateToLifeStorySetup = {
                    navController.navigate(Screen.LifeStorySetup.createRoute(patientId))
                }
            )
        }

        composable(
            route = Screen.LifeStorySetup.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            LifeStorySetupScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PatientProfile.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            MemoryGalleryScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PatientMatching.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            PatientMatchingScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CompanionVoice.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType; defaultValue = "default" }
            )
        ) { backStack ->
            val patientId = backStack.arguments?.getString("patientId") ?: ""
            val mode = backStack.arguments?.getString("mode") ?: "default"
            CompanionVoiceScreen(
                patientId = patientId,
                mode = mode,
                onNavigateToGames = {
                    navController.navigate(Screen.GameHub.createRoute(patientId)) {
                        popUpTo(Screen.CompanionVoice.createRoute(patientId)) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FamilyCircle.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStack ->
            com.nercare.cogcare.presentation.family.FamilyCircleScreen(
                patientId = backStack.arguments?.getString("patientId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
