package com.example.myapp.screens

//package com.example.campussecuritysystem.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun ResolvedAlertsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var resolvedAlerts by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("sos_alerts")
            .whereEqualTo("status", "resolved")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(
                        "ResolvedAlertsScreen",
                        "Error fetching resolved alerts: ${error.message}"
                    )
                    Toast.makeText(
                        context,
                        "Error fetching resolved alerts: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val alertList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        data?.put("id", doc.id)
                        data
                    }
                    resolvedAlerts = alertList
                    isLoading = false
                    Log.d(
                        "ResolvedAlertsScreen",
                        "Fetched ${resolvedAlerts.size} resolved alerts"
                    )
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resolved Alerts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onBack
            ) {
                Text("Back")
            }
        }
        if (isLoading) {
            Text("Loading resolved alerts...")
        } else if (resolvedAlerts.isEmpty()) {
            Text("No resolved alerts found.")
        } else {
            LazyColumn {
                items(resolvedAlerts) { alert: Map<String, Any> ->
                    AlertCard(alert = alert, showActions = false)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: Map<String, Any>, showActions: Boolean = true) {
    val location = alert["location"] as? Map<String, Any>
    val latitude = location?.get("latitude")?.toString() ?: "N/A"
    val longitude = location?.get("longitude")?.toString() ?: "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SOS Alert",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Student ID: ${alert["studentId"] ?: "Unknown"}")
            Text("Location: ($latitude, $longitude)")
            Text("Message: ${alert["message"] ?: "No message"}")
            Text("Status: ${alert["status"] ?: "Unknown"}")
            Text("Created At: ${alert["createdAt"] ?: "Unknown"}")
        }
    }
}