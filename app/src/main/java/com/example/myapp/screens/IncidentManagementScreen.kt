package com.example.myapp.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp.AdminViewModel
import com.example.myapp.Incident
import com.example.myapp.LocationData
import com.example.myapp.ui.theme.MyAppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentManagementScreen(
    adminViewModel: AdminViewModel = viewModel(),
    navController: androidx.navigation.NavController
) {
    val incidents by adminViewModel.filteredIncidents.collectAsState()
    val isLoading by adminViewModel.isIncidentsLoading.collectAsState()
    val error by adminViewModel.incidentError.collectAsState()
    val context = LocalContext.current

    val searchQuery by adminViewModel.incidentSearchQuery.collectAsState()
    val selectedCampus by adminViewModel.incidentCampusFilter.collectAsState()
    val selectedStatus by adminViewModel.incidentStatusFilter.collectAsState()

    // Sample lists for filters - replace with dynamic data if needed
    val campusOptions = listOf("All Campuses", "Ritson", "Steve Biko", "ML Sultan") // Example
    val statusOptions = listOf("All Statuses", "Active", "In Progress", "Resolved")

    var showIncidentDetailDialog by remember { mutableStateOf<Incident?>(null) }
    var showStatusChangeDialog by remember { mutableStateOf<Incident?>(null) }

    // Effect to observe incidents when filters change (already handled by ViewModel's flatMapLatest)
    // LaunchedEffect(selectedCampus, selectedStatus) {
    // adminViewModel.observeIncidents() // ViewModel handles re-observation
    // }

    MyAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Manage Incidents") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { adminViewModel.onIncidentSearchQueryChanged(it) },
                    label = { Text("Search by type, description, ID...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Filter Dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Campus Filter
                    FilterDropdown(
                        label = "Campus",
                        options = campusOptions,
                        selectedOption = selectedCampus ?: "All Campuses",
                        onOptionSelected = { adminViewModel.onIncidentCampusFilterChanged(if (it == "All Campuses") null else it) },
                        modifier = Modifier.weight(1f)
                    )
                    // Status Filter
                    FilterDropdown(
                        label = "Status",
                        options = statusOptions,
                        selectedOption = selectedStatus ?: "All Statuses",
                        onOptionSelected = { adminViewModel.onIncidentStatusFilterChanged(if (it == "All Statuses") null else it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading && incidents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Text(
                        "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (incidents.isEmpty() && !isLoading) {
                    Text(
                        "No incidents found matching your criteria.",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(incidents, key = { it.id }) { incident ->
                            IncidentItemCard(
                                incident = incident,
                                onViewDetails = { showIncidentDetailDialog = incident },
                                onChangeStatus = { showStatusChangeDialog = incident }
                            )
                        }
                    }
                }
            }
        }
    }

    showIncidentDetailDialog?.let { incident ->
        IncidentDetailDialog(incident = incident, onDismiss = { showIncidentDetailDialog = null })
    }

    showStatusChangeDialog?.let { incident ->
        ChangeIncidentStatusDialog(
            incident = incident,
            onDismiss = { showStatusChangeDialog = null },
            onStatusChange = { newStatus ->
                adminViewModel.updateIncidentStatus(incident.id, newStatus) { success, message ->
                    Toast.makeText(context, message ?: (if (success) "Status updated" else "Update failed"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        showStatusChangeDialog = null
                        // List will auto-update due to real-time listener
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IncidentItemCard(
    incident: Incident,
    onViewDetails: () -> Unit,
    onChangeStatus: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }, // Click card to view details
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(incident.type.replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.titleMedium)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                menuExpanded = false
                                onViewDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Status") },
                            onClick = {
                                menuExpanded = false
                                onChangeStatus()
                            }
                        )
                    }
                }
            }
            Text("ID: ${incident.id}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            Spacer(modifier = Modifier.height(4.dp))
            Text(incident.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Status: ${incident.status.replaceFirstChar { it.titlecase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Campus: ${incident.campusId}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            incident.createdAt?.let {
                val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                Text(
                    "Reported: ${sdf.format(it.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            incident.updatedAt?.let {
                val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                Text(
                    "Updated: ${sdf.format(it.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IncidentDetailDialog(incident: Incident, onDismiss: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) { // Using Dialog for more custom content flexibility
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp).heightIn(max = 600.dp)) { // Max height for scroll
                Text("Incident Details", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { DetailRow("ID:", incident.id) }
                    item { DetailRow("Type:", incident.type.replaceFirstChar { it.titlecase() }) }
                    item { DetailRow("Description:", incident.description) }
                    item { DetailRow("Status:", incident.status.replaceFirstChar { it.titlecase() }) }
                    item { DetailRow("Campus ID:", incident.campusId) }
                    item { DetailRow("Reported By:", incident.reportedBy) } // Assuming this is an ID or email
                    item { DetailRow("Assigned To:", incident.assignedTo.ifBlank { "N/A" }) }
                    item {
                        DetailRow("Created At:", incident.createdAt?.toDate()?.let { sdf.format(it) } ?: "N/A")
                    }
                    item {
                        DetailRow("Last Updated:", incident.updatedAt?.toDate()?.let { sdf.format(it) } ?: "N/A")
                    }

                    incident.location?.let { loc ->
                        item {
                            Text("Location:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Lat: ${loc.latitude}, Lon: ${loc.longitude}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                val incidentLocation = LatLng(loc.latitude, loc.longitude)
                                val cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(incidentLocation, 15f)
                                }
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState
                                ) {
                                    Marker(
                                        state = com.google.maps.android.compose.MarkerState(position = incidentLocation),
                                        title = incident.type,
                                        snippet = "Status: ${incident.status}"
                                    )
                                }
                            }
                        }
                    }

                    if (incident.images.isNotEmpty()) {
                        item {
                            Text("Images:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                            // Here you would display images, e.g., using Coil library
                            incident.images.forEach { imageUrl ->
                                Text(imageUrl, style = MaterialTheme.typography.bodySmall) // Placeholder
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
        Text(value)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIncidentStatusDialog(
    incident: Incident,
    onDismiss: () -> Unit,
    onStatusChange: (newStatus: String) -> Unit
) {
    val statusOptions = listOf("Active", "In Progress", "Resolved") // User-friendly options
    // Map to values expected by backend if different (e.g., "active", "in_progress", "resolved")
    val statusValueMapping = mapOf(
        "Active" to "active",
        "In Progress" to "in_progress",
        "Resolved" to "resolved"
    )

    var selectedStatusDisplay by remember { mutableStateOf(incident.status.replaceFirstChar { it.titlecase() }) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Incident Status") },
        text = {
            Column {
                Text("Incident: ${incident.type} (ID: ${incident.id.take(8)}...)")
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatusDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("New Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    selectedStatusDisplay = status
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val statusToSend = statusValueMapping[selectedStatusDisplay] ?: selectedStatusDisplay.lowercase()
                onStatusChange(statusToSend)
            }) {
                Text("Update Status")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
