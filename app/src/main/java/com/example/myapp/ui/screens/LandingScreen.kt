package com.example.myapp.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.Incident
import com.example.myapp.R
import com.example.myapp.User
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    navController: NavController
) {
    var incidents by remember { mutableStateOf<List<Incident>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    // Fetch incidents
    LaunchedEffect(Unit) {
        fetchIncidents(
            onSuccess = { list ->
                incidents = list
                isLoading = false
            },
            onError = { message ->
                errorMessage = message
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.dut_logo),
                                contentDescription = "DUT Logo",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Campus Safety",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1C) // Using your on_surface color
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = "SOS",
                            tint = Color.White, // White icon for better contrast
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White, // Using your surface_color
                    titleContentColor = Color(0xFF1C1C1C), // Using your on_surface color
                    actionIconContentColor = Color(0xFF1C1C1C) // Using your on_surface color
                ),
                modifier = Modifier.padding(horizontal = 16.dp) // Add horizontal padding
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                isLoading = true
                fetchIncidents(
                    onSuccess = { list ->
                        incidents = list
                        isLoading = false
                    },
                    onError = { message ->
                        errorMessage = message
                        isLoading = false
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Welcome Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Welcome to DUT Campus Safety",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Stay alert. Stay informed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Incidents Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                ) {
                    Text(
                        "Recent Incidents",
                        style = MaterialTheme.typography.titleLarge, // Made larger
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        errorMessage != null -> {
                            ErrorMessage(errorMessage!!)
                        }

                        incidents.isEmpty() -> {
                            EmptyIncidentsView()
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(incidents) { incident ->
                                    IncidentCard(incident = incident)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Get Started", style = MaterialTheme.typography.labelLarge)
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dut_logo),
                        contentDescription = "Secure",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Your data is encrypted and secure",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun fetchIncidents(
    onSuccess: (List<Incident>) -> Unit,
    onError: (String) -> Unit
) {
    val db = Firebase.firestore
    db.collection("incidents")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(5)
        .get()
        .addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Incident::class.java)?.copy(id = doc.id)
            }
            onSuccess(list)
        }
        .addOnFailureListener { error ->
            onError("Failed to load incidents")
            Log.e("LandingScreen", "Error fetching incidents", error)
        }
}

@Composable
private fun IncidentCard(incident: Incident) {
    val formattedDate = remember(incident.createdAt) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(incident.createdAt?.toDate() ?: Date())
    }

    var reporterName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(incident.reportedBy) {
        fetchUserById(
            userId = incident.reportedBy,
            onSuccess = { user ->
                reporterName = user.name.ifEmpty { "Anonymous" }
            },
            onError = { _ ->
                reporterName = "Unknown User"
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = incident.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (incident.type.lowercase()) {
                        "emergency" -> MaterialTheme.colorScheme.error
                        "warning" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.dut_logo),
                    contentDescription = "Reported by",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reported by $reporterName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyIncidentsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.dut_logo),
            contentDescription = "No incidents",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recent incidents",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

fun fetchUserById(
    userId: String,
    onSuccess: (User) -> Unit,
    onError: (String) -> Unit
) {
    val db = Firebase.firestore
    db.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            document.toObject(User::class.java)?.let { user ->
                onSuccess(user.copy(id = document.id))
            } ?: run {
                onError("User not found")
            }
        }
        .addOnFailureListener { error ->
            onError("Failed to load user: ${error.localizedMessage}")
        }
}