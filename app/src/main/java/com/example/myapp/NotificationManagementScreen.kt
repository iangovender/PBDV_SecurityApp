package com.example.myapp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp.AdminViewModel
import com.example.myapp.Notification // Your Notification data class
import com.example.myapp.ui.theme.MyAppTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagementScreen(
    adminViewModel: AdminViewModel = viewModel(),
    navController: androidx.navigation.NavController // For back navigation
) {
    val notifications by adminViewModel.notifications.collectAsState()
    val isLoading by adminViewModel.isNotificationsLoading.collectAsState()
    val error by adminViewModel.notificationError.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }

    MyAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Manage Notifications") },
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Notification")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (isLoading && notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Text(
                        "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Button(onClick = { adminViewModel.fetchNotifications() }) {
                    Text("Refresh Notifications")
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (notifications.isEmpty() && !isLoading) {
                    Text(
                        "No notifications found.",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    NotificationList(notifications = notifications)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateNotificationDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, message, type, targetRole ->
                adminViewModel.createAndSendNotification(
                    title = title,
                    message = message,
                    type = type,
                    targetRole = targetRole,
                    // campusId = "your_default_or_selected_campus_id", // Optional: Add campus selection
                    onResult = { success, resultMessage ->
                        Toast.makeText(context, resultMessage ?: (if (success) "Action successful" else "Action failed"), Toast.LENGTH_LONG).show()
                        if (success) {
                            showCreateDialog = false
                            adminViewModel.fetchNotifications() // Refresh list after creating
                        }
                    }
                )
            }
        )
    }
}

@Composable
fun NotificationList(notifications: List<Notification>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(notifications, key = { it.id }) { notification ->
            NotificationItem(notification = notification)
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(notification.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(notification.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Type: ${notification.type.replaceFirstChar { it.titlecase() }}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Target: ${notification.targetRole.replaceFirstChar { it.titlecase() }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            notification.createdAt?.let {
                val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                Text(
                    "Sent: ${sdf.format(it.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (notification.campusId.isNotBlank() && notification.campusId != "all_campuses") {
                Text(
                    "Campus: ${notification.campusId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotificationDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, message: String, type: String, targetRole: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val notificationTypes = listOf("General", "Incident", "SOS", "Alert", "Reminder")
    var selectedType by remember { mutableStateOf(notificationTypes[0]) }
    var typeExpanded by remember { mutableStateOf(false) }

    val targetRoles = listOf("All Users", "Student", "Security", "Admin") // "all_users", "student", "security", "admin"
    var selectedRole by remember { mutableStateOf(targetRoles[0]) }
    var roleExpanded by remember { mutableStateOf(false) }

    // Used by AdminViewModel: "all_users", "student", "security", "admin"
    // Map display names to values for sending to ViewModel
    val roleMapping = mapOf(
        "All Users" to "all_users",
        "Student" to "student",
        "Security" to "security",
        "Admin" to "admin"
    )


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )

                // Notification Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Notification Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        notificationTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        targetRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val roleValue = roleMapping[selectedRole] ?: selectedRole.lowercase(Locale.getDefault())
                onSubmit(title, message, selectedType.lowercase(Locale.getDefault()), roleValue)
            }) {
                Text("Send Notification")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}