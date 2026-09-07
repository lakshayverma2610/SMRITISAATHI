package com.nercare.cogcare.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home)
    object Games : BottomNavItem(Screen.GameHub.route, "Games", Icons.Filled.PlayArrow)
    // Using Face icon as placeholder for Memories/Friends
    object Memories : BottomNavItem(Screen.PatientProfile.route, "Memories", Icons.Filled.Face)
    object Reminders : BottomNavItem(Screen.Reminders.route, "Reminders", Icons.Filled.Notifications)
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    patientId: String?,
    content: @Composable (Modifier) -> Unit
) {
    // Only show bottom bar if we have a logged-in patient and we're not on the onboarding screen
    val showBottomBar = patientId != null && patientId.isNotEmpty()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController, patientId = patientId!!)
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    patientId: String
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Games,
        BottomNavItem.Memories,
        BottomNavItem.Reminders
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show the bottom bar if the current destination is one of the top-level items
    // (This hides the bottom bar when deep inside a game)
    val isTopLevelDestination = items.any { it.route.substringBefore("/") == currentDestination?.route?.substringBefore("/") }

    if (!isTopLevelDestination) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route?.substringBefore("/") == item.route.substringBefore("/") } == true
            
            // Build the concrete route with the patientId
            val concreteRoute = item.route.replace("{patientId}", patientId)

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    navController.navigate(concreteRoute) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
