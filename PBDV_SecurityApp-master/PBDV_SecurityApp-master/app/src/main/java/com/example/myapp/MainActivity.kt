package com.example.myapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapp.screens.LocationScreen
import com.example.myapp.screens.CommunicationScreen
import com.example.myapp.screens.ReportIncidentScreen
import com.example.myapp.screens.ResolvedAlertsScreen
import com.example.myapp.screens.SecurityDashboardScreen
import com.example.myapp.screens.SecurityLoginScreen
import com.example.myapp.screens.StudentLoginScreen
import com.example.myapp.screens.StudentPanicScreen
import com.example.myapp.ui.theme.MyAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Retrieve the destination from the intent, default to "location"
        val startDestination = intent.getStringExtra("destination") ?: "location"
        setContent {
            MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
fun MyApp(startDestination: String = "location") {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        //  Authentication
        composable("login") {
            LoginScreen(
                onSignInSuccess = { user ->
                    when (user.role) {
                        "student" -> {
                            val intent = Intent(navController.context, StudentDashboardActivity::class.java)
                            navController.context.startActivity(intent)
                        }
                        "security" -> {
                            val intent = Intent(navController.context, MainActivity::class.java).apply {
                                putExtra("destination", "security")
                            }
                            navController.context.startActivity(intent)
                        }
                        "admin" -> {
                            val intent = Intent(navController.context, AdminDashboardActivity::class.java)
                            navController.context.startActivity(intent)
                        }
                    }
                },
                onSignUpClick = {
                    navController.navigate("signup")
                },
                onNavigateToForgotPassword = {
                    val intent = Intent(navController.context, ForgotPasswordActivity::class.java)
                    navController.context.startActivity(intent)
                }
            )
        }

        composable("signup") {
            // Placeholder for SignupScreen
            // This will navigate back to login upon successful registration
            SignupScreen(
                onSignUpSuccess = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        // Note: The "map" route is commented out as the first segment's LocationScreen.kt
        // does not navigate to a map screen. Uncomment if needed for other parts of the app.
        /*
        composable("map") {
            MapScreen()
        }
        */

        // For students
        composable("student_dashboard") {
            SimplifiedStudentDashboardScreen(
                onGetLocation = { navController.navigate("location") },
                onReportIncident = { navController.navigate("reportIncident") }
            )
        }

        composable("location") {
            LocationScreen(
                onAlertSent = { alertId ->
                    navController.navigate("communication/$alertId/student")
                }
            )
        }

        composable("reportIncident") {
            ReportIncidentScreen(
                onSubmit = {
                    navController.popBackStack() // Navigate back to the dashboard after submitting the report
                }
            )
        }

        composable("student") {
            StudentPanicScreen(
                onAlertSent = { alertId ->
                    navController.navigate("communication/$alertId/student")
                }
            )
        }

        composable("communication/{alertId}/{sender}") { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            val sender = backStackEntry.arguments?.getString("sender") ?: "student"
            CommunicationScreen(
                alertId = alertId,
                sender = sender,
                onBack = {
                    val destination = if (sender == "student") "student" else "security"
                    navController.navigate(destination)
                }
            )
        }

        //  For security
        composable("security_login") {
            SecurityLoginScreen(
                onLoginSuccess = { navController.navigate("security") }
            )
        }
        composable("security") {
            SecurityDashboardScreen(
                onLogout = {
                    // Start LoginActivity and finish MainActivity
                    val intent = Intent(navController.context, Class.forName("com.example.myapp.LoginActivity"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    navController.context.startActivity(intent)
                    (navController.context as ComponentActivity).finish()
                },
                onCommunicate = { alertId ->
                    navController.navigate("communication/$alertId/security")
                }
            )
        }
        composable("resolved") {
            ResolvedAlertsScreen(
                onBack = { navController.navigate("security") }
            )
        }
    }
}