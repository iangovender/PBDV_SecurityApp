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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapp.screens.CommunicationScreen
import com.example.myapp.screens.IncidentViewScreen
import com.example.myapp.screens.LocationScreen
import com.example.myapp.screens.MapScreen
import com.example.myapp.screens.ResolvedAlertsScreen
import com.example.myapp.screens.SecurityLoginScreen
import com.example.myapp.screens.StudentPanicScreen
import com.example.myapp.ui.screens.LandingScreen
import com.example.myapp.ui.screens.LoginScreen
import com.example.myapp.ui.screens.SignupScreen
import com.example.myapp.ui.screens.SplashScreen
import com.example.myapp.ui.screens.security.SecurityDashboardScreen
import com.example.myapp.ui.screens.student.ReportIncidentScreen
import com.example.myapp.ui.theme.MyAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Retrieve the destination from the intent, default to "splash"
        val startDestination = if (intent.hasExtra("destination")) {
            intent.getStringExtra("destination") ?: "splash"
        } else "splash"

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
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("landing") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("landing") {
            LandingScreen(
                navController = navController
            )
        }
        // Authentication
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

        // For students
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
                    navController.popBackStack()
                },
                navController
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

        // For security
        composable("security_login") {
            SecurityLoginScreen(
                onLoginSuccess = { navController.navigate("security") }
            )
        }

        composable("security") {
            SecurityDashboardScreen(
                navController = navController,
                onLogout = {
                    // Start LoginActivity and finish MainActivity
                    val intent = Intent(navController.context, Class.forName("com.example.myapp.LoginActivity"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    navController.context.startActivity(intent)
                    (navController.context as ComponentActivity).finish()
                },
                onCommunicate = { alertId ->
                    navController.navigate("communication/$alertId/security")
                },
                onViewIncidents = {
                    navController.navigate("incident_view")
                }
            )
        }

        composable("incident_view") {
            IncidentViewScreen()
        }

        composable("resolved") {
            ResolvedAlertsScreen(
                onBack = { navController.navigate("security") }
            )
        }

        composable("map_screen/{alertId}") { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId")
            MapScreen(
                alertId = alertId ?: "",
                onBack = {
                    navController.navigate("security") {
                        popUpTo("map_screen/{alertId}") { inclusive = true }
                    }
                }
            )
        }
    }
}
