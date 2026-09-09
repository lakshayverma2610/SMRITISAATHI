package com.nercare.cogcare.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object RoleSelection : Screen("role_selection")
    object CaregiverAuth : Screen("caregiver_auth")
    object PatientSelection : Screen("patient_selection")
    object PatientLogin : Screen("patient_login")
    object Onboarding : Screen("onboarding/{caregiverId}") {
        fun createRoute(caregiverId: String) = "onboarding/$caregiverId"
    }
    object Home : Screen("home/{patientId}") {
        fun createRoute(patientId: String) = "home/$patientId"
    }
    object GameHub : Screen("game_hub/{patientId}") {
        fun createRoute(patientId: String) = "game_hub/$patientId"
    }
    object MemoryCardGame : Screen("memory_game/{patientId}/{difficulty}") {
        fun createRoute(patientId: String, difficulty: Int = 2) = "memory_game/$patientId/$difficulty"
    }
    object SequenceGame : Screen("sequence_game/{patientId}/{difficulty}") {
        fun createRoute(patientId: String, difficulty: Int = 2) = "sequence_game/$patientId/$difficulty"
    }
    object PatternGame : Screen("pattern_game/{patientId}/{difficulty}") {
        fun createRoute(patientId: String, difficulty: Int = 2) = "pattern_game/$patientId/$difficulty"
    }
    object WordAssocGame : Screen("word_assoc_game/{patientId}/{difficulty}") {
        fun createRoute(patientId: String, difficulty: Int = 2) = "word_assoc_game/$patientId/$difficulty"
    }
    object RoutineGame : Screen("routine_game/{patientId}/{difficulty}") {
        fun createRoute(patientId: String, difficulty: Int = 2) = "routine_game/$patientId/$difficulty"
    }
    object GenericCognitiveGame : Screen("cognitive_game/{patientId}/{gameId}/{gameTitle}/{domain}") {
        fun createRoute(patientId: String, gameId: String, gameTitle: String, domain: String) =
            "cognitive_game/$patientId/$gameId/${java.net.URLEncoder.encode(gameTitle, "UTF-8")}/${java.net.URLEncoder.encode(domain, "UTF-8")}"
    }
    object Reminders : Screen("reminders/{patientId}") {
        fun createRoute(patientId: String) = "reminders/$patientId"
    }
    object CaregiverPin : Screen("caregiver_pin/{patientId}") {
        fun createRoute(patientId: String) = "caregiver_pin/$patientId"
    }
    object CaregiverDashboard : Screen("caregiver_dashboard/{patientId}") {
        fun createRoute(patientId: String) = "caregiver_dashboard/$patientId"
    }
    object EditPatient : Screen("edit_patient/{patientId}") {
        fun createRoute(patientId: String) = "edit_patient/$patientId"
    }
    object PatientProfile : Screen("patient_profile/{patientId}") {
        fun createRoute(patientId: String) = "patient_profile/$patientId"
    }
    object AddReminder : Screen("add_reminder/{patientId}") {
        fun createRoute(patientId: String) = "add_reminder/$patientId"
    }
    object EditReminder : Screen("edit_reminder/{patientId}/{reminderId}") {
        fun createRoute(patientId: String, reminderId: String) = "edit_reminder/$patientId/$reminderId"
    }
    object LifeStorySetup : Screen("life_story_setup/{patientId}") {
        fun createRoute(patientId: String) = "life_story_setup/$patientId"
    }
    object CompanionVoice : Screen("companion_voice/{patientId}?mode={mode}") {
        fun createRoute(patientId: String, mode: String = "default") = "companion_voice/$patientId?mode=$mode"
    }
    object PatientMatching : Screen("patient_matching/{patientId}") {
        fun createRoute(patientId: String) = "patient_matching/$patientId"
    }
    object FamilyCircle : Screen("family_circle/{patientId}") {
        fun createRoute(patientId: String) = "family_circle/$patientId"
    }
}
