package com.example.myapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Data class for incidents with Timestamp
data class Incident(
    val id: String = "",
    val incidentType: String = "",
    val description: String = "",
    val location: Map<String, Double>? = null,
    val createdAt: Timestamp? = null,
    val status: String = "",
    val studentId: String = ""
)

@Composable
fun IncidentViewScreen() {
    val context = LocalContext.current
    val db = Firebase.firestore
    val currentUser = FirebaseAuth.getInstance().currentUser

    // State for incidents and loading
    var incidents by remember { mutableStateOf<List<Incident>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }

    // Fetch user role
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val userSnapshot = db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                val user = userSnapshot.data
                userRole = user?.get("role") as? String
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching user role: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Fetch incidents
    LaunchedEffect(Unit) {
        try {
            db.collection("incidents")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    isLoading = false
                    if (e != null) {
                        error = "Failed to load incidents: ${e.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        incidents = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data
                            Incident(
                                id = doc.id,
                                incidentType = data?.get("incidentType") as? String ?: "",
                                description = data?.get("description") as? String ?: "",
                                location = data?.get("location") as? Map<String, Double>,
                                createdAt = data?.get("createdAt") as? Timestamp,
                                status = data?.get("status") as? String ?: "",
                                studentId = data?.get("studentId") as? String ?: ""
                            )
                        }
                        error = null
                    } else {
                        error = "No incidents found."
                    }
                }
        } catch (e: Exception) {
            isLoading = false
            error = "Error fetching incidents: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Incident Reports",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            incidents.isEmpty() -> {
                Text(
                    text = "No incidents found.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(incidents, key = { it.id }) { incident ->
                        IncidentItem(
                            incident = incident,
                            canResolve = userRole in listOf("security", "admin"),
                            onResolveClick = {
                                if (userRole !in listOf("security", "admin")) {
                                    Toast.makeText(context, "Only security or admin can resolve incidents", Toast.LENGTH_LONG).show()
                                    return@IncidentItem
                                }
                                resolveIncident(context, db, incident.id, incident.incidentType, incident.studentId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncidentItem(
    incident: Incident,
    canResolve: Boolean,
    onResolveClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Could expand for details if needed */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Type: ${incident.incidentType}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Description: ${incident.description}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Location: Lat ${incident.location?.get("latitude") ?: "N/A"}, Lon ${incident.location?.get("longitude") ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Created: ${incident.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${incident.status}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Student ID: ${incident.studentId}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (canResolve) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onResolveClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Resolve Incident")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resolve")
                }
            }
        }
    }
}

private fun resolveIncident(context: Context, db: FirebaseFirestore, incidentId: String, incidentType: String, studentId: String) {
    db.collection("incidents")
        .document(incidentId)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Incident resolved and deleted", Toast.LENGTH_SHORT).show()

            // Send push notification to security_team
            val notification = hashMapOf(
                "campusId" to "unknown", // Replace with actual campusId if available
                "title" to "Incident Resolved",
                "message" to "Incident ($incidentType) reported by student $studentId has been resolved.",
                "type" to "incident",
                "targetRole" to "security",
                "createdAt" to Timestamp.now()
            )
            db.collection("notifications")
                .add(notification)
                .addOnSuccessListener {
                    FirebaseMessaging.getInstance().subscribeToTopic("security_team")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to send notification: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to resolve incident: ${e.message}", Toast.LENGTH_LONG).show()
        }
}