package com.example.myapp

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
import com.example.myapp.screens.LocationScreen
import com.example.campussecuritysystem.ui.screens.CommunicationScreen
import com.example.campussecuritysystem.ui.screens.ResolvedAlertsScreen
import com.example.campussecuritysystem.ui.screens.SecurityDashboardScreen
import com.example.campussecuritysystem.ui.screens.SecurityLoginScreen
import com.example.campussecuritysystem.ui.screens.StudentLoginScreen
import com.example.campussecuritysystem.ui.screens.StudentPanicScreen
import com.example.myapp.ui.theme.MyAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "location") {
        composable("location") {
            LocationScreen()
        }
        composable("student") {
            StudentPanicScreen(
                onAlertSent = { alertId ->
                    navController.navigate("communication/$alertId/student")
                }
            )
        }
        composable("security_login") {
            SecurityLoginScreen(
                onLoginSuccess = { navController.navigate("security") }
            )
        }
        composable("security") {
            SecurityDashboardScreen(
                onLogout = { navController.navigate("security_login") },
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

    }
}