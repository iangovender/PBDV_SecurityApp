package com.example.myapp.ui.screens.security

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.NavController
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboardScreen(
    onLogout: () -> Unit,
    onCommunicate: (String) -> Unit,
    onViewIncidents: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    var alerts by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("sos_alerts")
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onViewIncidents,
                icon = { Icon(Icons.Default.List, "Incidents") },
                text = { Text("View Incidents") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    ErrorMessage(
                        message = errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                alerts.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                onCommunicate = { onCommunicate(alert["id"] as String) },
                                onViewOnMap = {
                                    navController.navigate("map_screen/${alert["id"]}") {
                                        popUpTo("security") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: Map<String, Any>,
    onResolve: () -> Unit,
    onCommunicate: () -> Unit,
    onViewOnMap: () -> Unit
) {
    val location = alert["location"] as? Map<String, Any>
    val latitude = location?.get("latitude")?.toString() ?: "N/A"
    val longitude = location?.get("longitude")?.toString() ?: "N/A"
    val createdAt = alert["createdAt"]?.toString()?.let {
        try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it)
                ?.let { date -> SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date) }
        } catch (e: Exception) {
            it
        }
    } ?: "Unknown"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOS Alert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = createdAt,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AlertDetailItem(
                icon = Icons.Default.Person,
                label = "Student ID",
                value = alert["studentId"]?.toString() ?: "Unknown"
            )
            AlertDetailItem(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = "($latitude, $longitude)"
            )
            AlertDetailItem(
                icon = Icons.Default.Message,
                label = "Message",
                value = alert["message"]?.toString() ?: "No message"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResolve,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Resolve",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resolve")
                }

                OutlinedButton(
                    onClick = onCommunicate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Communicate",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat")
                }

                OutlinedButton(
                    onClick = onViewOnMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "View on Map",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map")
                }
            }
        }
    }
}

@Composable
fun AlertDetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ErrorMessage(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = "No alerts",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No pending SOS alerts",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}