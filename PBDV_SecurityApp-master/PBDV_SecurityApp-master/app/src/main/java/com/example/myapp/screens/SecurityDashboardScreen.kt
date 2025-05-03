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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityDashboardScreen(onLogout: () -> Unit, onCommunicate: (String) -> Unit) {
    val context = LocalContext.current
    var alerts by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("sos_alerts")
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SecurityDashboardScreen", "Error fetching alerts: ${error.message}")
                    errorMessage = "Error fetching alerts: ${error.message}"
                    return@addSnapshotListener
                }
                errorMessage = null
                if (snapshot != null) {
                    val alertList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        data?.put("id", doc.id)
                        data
                    }
                    alerts = alertList
                    alertList.forEach { alert ->
                        Log.d("SecurityDashboardScreen", "Alert ID: ${alert["id"]} | CreatedAt: ${alert["createdAt"]}")
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Security Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout", color = Color.White)
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else if (alerts.isEmpty()) {
            Text(
                text = "No pending SOS alerts",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(
                        alert = alert,
                        onResolve = {
                            val db = Firebase.firestore
                            db.collection("sos_alerts")
                                .document(alert["id"] as String)
                                .update(
                                    mapOf(
                                        "status" to "resolved",
                                        "updatedAt" to SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()
                                        ).format(Date())
                                    )
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Alert marked as resolved",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Failed to resolve alert: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onCommunicate = { onCommunicate(alert["id"] as String) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: Map<String, Any>, onResolve: () -> Unit, onCommunicate: () -> Unit) {
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
            Button(
                onClick = onResolve,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Resolve Alert")
            }
            Button(
                onClick = onCommunicate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Communicate with Student")
            }
        }
    }
}