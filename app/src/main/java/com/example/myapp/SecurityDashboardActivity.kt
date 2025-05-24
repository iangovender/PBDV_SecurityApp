package com.example.myapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.myapp.ui.screens.security.SecurityDashboardScreen
import com.example.myapp.ui.theme.MyAppTheme

class SecurityDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background  
                ) {
                    SecurityDashboardScreen(
                        onLogout = {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        onCommunicate = { alertId ->
                            println("Communicate with alert $alertId")
                        },
                        onViewIncidents = {
                            println("Navigate to incidents screen")
                        },
                        navController = navController
                    )
                }
            }
        }
    }
}
