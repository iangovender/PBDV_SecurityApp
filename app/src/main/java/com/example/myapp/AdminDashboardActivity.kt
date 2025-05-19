package com.example.myapp // Or com.example.myapp.admin if you prefer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapp.screens.IncidentManagementScreen // Import IncidentManagementScreen
import com.example.myapp.NotificationManagementScreen
import com.example.myapp.ui.screens.admin.AdminDashboardScreen
import com.example.myapp.ui.theme.MyAppTheme
import com.google.firebase.auth.FirebaseAuth

// Define navigation routes for the admin section
object AdminDestinations {
    const val DASHBOARD = "admin_dashboard"
    const val USER_MANAGEMENT = "user_management"
    const val NOTIFICATION_MANAGEMENT = "notification_management"
    const val INCIDENT_MANAGEMENT = "incident_management" // New route for incidents
}

class AdminDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminAppNavigation()
                }
            }
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Composable
    fun AdminAppNavigation() {
        val navController = rememberNavController()
        // ViewModel scoped to the NavHost, shared across admin screens
        val adminViewModel: AdminViewModel = viewModel()

        NavHost(
            navController = navController,
            startDestination = AdminDestinations.DASHBOARD
        ) {
            composable(AdminDestinations.DASHBOARD) {
                AdminDashboardScreen(
                    adminViewModel = adminViewModel,
                    onNavigateToUserManagement = {
                        navController.navigate(AdminDestinations.USER_MANAGEMENT)
                    },
                    onNavigateToNotificationManagement = {
                        navController.navigate(AdminDestinations.NOTIFICATION_MANAGEMENT)
                    },
                    onNavigateToIncidentManagement = { // Pass the new navigation lambda for incidents
                        navController.navigate(AdminDestinations.INCIDENT_MANAGEMENT)
                    },
                    onLogout = {
                        logout()
                    }
                )
            }
            composable(AdminDestinations.USER_MANAGEMENT) {
                UserManagementScreen(
                    adminViewModel = adminViewModel,
                    navController = navController
                )
            }
            composable(AdminDestinations.NOTIFICATION_MANAGEMENT) {
                NotificationManagementScreen(
                    adminViewModel = adminViewModel,
                    navController = navController
                )
            }
            composable(AdminDestinations.INCIDENT_MANAGEMENT) { // New composable destination for incidents
                IncidentManagementScreen(
                    adminViewModel = adminViewModel,
                    navController = navController
                )
            }
        }
    }
}