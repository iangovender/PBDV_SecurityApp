package com.example.myapp.ui.screens.student

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.myapp.Incident
import com.example.myapp.R
import com.example.myapp.SosAlert
import com.example.myapp.User
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    onGetLocation: () -> Unit,
    onReportIncident: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var incidentList by remember { mutableStateOf<List<Incident>>(emptyList()) }
    var sosAlerts by remember { mutableStateOf<List<SosAlert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingSos by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isRefreshing = isLoading || isLoadingSos

    fun fetchIncidents() {
        isLoading = true
        db.collection("incidents")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                incidentList = result.mapNotNull { it.toObject(Incident::class.java) }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                errorMessage = context.getString(R.string.error_loading_incidents, exception.message)
                isLoading = false
            }
    }

    fun fetchSosAlerts() {
        isLoadingSos = true
        errorMessage = null // Clear previous errors

        db.collection("sos_alerts")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // Handle empty case if needed
                    sosAlerts = emptyList()
                } else {
                    sosAlerts = result.mapNotNull { document ->
                        try {
                            document.toObject(SosAlert::class.java).also {
                                // Ensure document ID is preserved if needed
                                it.studentId = document.id
                            }
                        } catch (e: Exception) {
                            Log.e("FetchSOS", "Error parsing document ${document.id}", e)
                            null
                        }
                    }
                }
                isLoadingSos = false
            }
            .addOnFailureListener { exception ->
                errorMessage = when (exception) {
                    is FirebaseFirestoreException -> {
                        when (exception.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                context.getString(R.string.error_permission_denied)
                            FirebaseFirestoreException.Code.UNAVAILABLE ->
                                context.getString(R.string.error_network_unavailable)
                            else ->
                                context.getString(R.string.error_loading_sos, exception.localizedMessage)
                        }
                    }
                    else -> context.getString(R.string.error_loading_sos, exception.localizedMessage)
                }
                isLoadingSos = false
                Log.e("FetchSOS", "Error fetching alerts", exception)
            }
    }

    LaunchedEffect(Unit) {
        fetchIncidents()
        fetchSosAlerts()
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dut_logo),
                            contentDescription = "DUT Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Campus Safety",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        FilledTonalButton(
                            onClick = onGetLocation,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Emergency,
                                contentDescription = "Emergency",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("SOS")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->


            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    fetchIncidents()
                    fetchSosAlerts()
                },
                modifier = Modifier.padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Emergency Contacts Section
                    item {
                        Text(
                            text = "Emergency Contacts",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        EmergencyContactsGrid(LocalContext.current) // Replace with non-scrollable grid
                    }

                    // Divider
                    item {
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    // Safety Tips Section
                    item {
                        SafetyTipsSection()
                    }

                    // Divider
                    item {
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    // SOS Alerts Section
                    if (sosAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active Alerts",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(sosAlerts) { alert ->
                            SosAlertCard(alert)
                        }
                    } else if (!isLoadingSos) {
                        item {
                            Text(
                                text = "No active alerts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Divider
                    item {
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    // Incidents Section
                    item {
                        Text(
                            text = "Recent Incidents",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (incidentList.isEmpty() && !isLoading) {
                        item {
                            EmptyIncidentsView()
                        }
                    } else {
                        items(incidentList) { incident ->
                            IncidentCard(incident)
                        }
                    }

                    item {
                        Button(
                            onClick = onReportIncident,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Report Incident", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Error Message
                    errorMessage?.let {
                        item {
                            ErrorMessage(it)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EmergencyContactsGrid(context: Context) {
    val contacts = listOf(
        Pair("Campus Security", "+27 31 373 2181"),
        Pair("Police", "10111"),
        Pair("Ambulance", "10177"),
        Pair("Fire", "10111")
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // First row
        Row {
            EmergencyContactItem(
                name = contacts[0].first,
                number = contacts[0].second,
                onClick = { makePhoneCall(context, contacts[0].second) }
            )
            Spacer(Modifier.width(8.dp))
            EmergencyContactItem(
                name = contacts[1].first,
                number = contacts[1].second,
                onClick = { makePhoneCall(context, contacts[1].second) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Second row
        Row {
            EmergencyContactItem(
                name = contacts[2].first,
                number = contacts[2].second,
                onClick = { makePhoneCall(context, contacts[2].second) }
            )
            Spacer(Modifier.width(8.dp))
            EmergencyContactItem(
                name = contacts[3].first,
                number = contacts[3].second,
                onClick = { makePhoneCall(context, contacts[3].second) }
            )
        }
    }
}

@Composable
private fun EmergencyContactItem(name: String, number: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(125.dp),  // Give a fixed width
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call $name",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = number,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SafetyTipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Safety Tips",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val tips = listOf(
            "Be aware of your surroundings",
            "Walk in well-lit areas at night",
            "Save emergency contacts",
            "Trust your instincts"
        )

        tips.forEach { tip ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SosAlertCard(alert: SosAlert) {
    val formattedTime = remember(alert.createdAt) {
        alert.createdAt?.toDate()?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
        } ?: "Unknown time"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Emergency,
                    contentDescription = "Emergency",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EMERGENCY ALERT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.message.ifEmpty { "Emergency alert" },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Status: ${alert.status.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Posted $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

fun fetchSosAlerts(
    onSuccess: (List<SosAlert>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("sos_alerts")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(5)
        .get()
        .addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SosAlert::class.java)?.copy()
            }
            onSuccess(list)
        }
        .addOnFailureListener { error ->
            onError("Failed to load SOS alerts: ${error.localizedMessage}")
            Log.e("StudentDashboard", "Error fetching SOS alerts", error)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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

fun makePhoneCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
        == PackageManager.PERMISSION_GRANTED
    ) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Call permission not granted", Toast.LENGTH_SHORT).show()
    }
}

fun fetchUserById(
    userId: String,
    onSuccess: (User) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
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