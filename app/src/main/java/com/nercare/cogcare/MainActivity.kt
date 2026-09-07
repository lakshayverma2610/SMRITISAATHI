package com.nercare.cogcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import androidx.core.content.ContextCompat
import com.nercare.cogcare.presentation.navigation.AppNavigation
import com.nercare.cogcare.presentation.navigation.MainAppScaffold
import com.nercare.cogcare.presentation.navigation.Screen
import com.nercare.cogcare.presentation.theme.CogCareTheme
import com.nercare.cogcare.sync.SyncWorker
import com.nercare.cogcare.reminder.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule background sync
        SyncWorker.schedule(workManager)

        val reminderPatientId = intent.getStringExtra(ReminderScheduler.EXTRA_PATIENT_ID)
        val initialRoute = if (
            intent.getStringExtra("open_screen") == "reminders" && !reminderPatientId.isNullOrBlank()
        ) {
            Screen.Reminders.createRoute(reminderPatientId)
        } else {
            Screen.Splash.route
        }

        setContent {
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            CogCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Extract patientId from the current route to pass to the bottom bar
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val patientId = navBackStackEntry?.arguments?.getString("patientId")
                    
                    MainAppScaffold(
                        navController = navController,
                        patientId = patientId
                    ) { modifier ->
                        AppNavigation(
                            navController = navController,
                            startDestination = initialRoute,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}
