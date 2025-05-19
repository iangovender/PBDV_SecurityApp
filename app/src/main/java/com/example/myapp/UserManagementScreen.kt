package com.example.myapp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp.ui.theme.MyAppTheme
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

@Composable
fun UserManagementScreen(
    adminViewModel: AdminViewModel = viewModel(),
    navController: androidx.navigation.NavController
) {
    val users by adminViewModel.users.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val error by adminViewModel.error.collectAsState()
    val selectedUser by adminViewModel.selectedUser.collectAsState()

    var showUserDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUserDetailsDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var userToView by remember { mutableStateOf<User?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCampus by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var expandedCampusDropdown by remember { mutableStateOf(false) }
    var expandedRoleDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val campuses = listOf("Ritson", "Steve Biko", "ML Sultan")
    val roles = listOf("student", "security", "admin")

    // Fetch users and apply filters
    LaunchedEffect(searchQuery, selectedCampus, selectedRole) {
        adminViewModel.fetchUsers(
            searchQuery = searchQuery,
            campusId = selectedCampus,
            role = selectedRole
        )
    }

    MyAppTheme {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Custom Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "User Management",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Main Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search by name or email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Filter Dropdowns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Campus Filter
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedCampus ?: "All Campuses",
                                    onValueChange = { },
                                    label = { Text("Campus") },
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Expand campus dropdown",
                                            modifier = Modifier.clickable { expandedCampusDropdown = !expandedCampusDropdown }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedCampusDropdown = !expandedCampusDropdown }
                                )
                                DropdownMenu(
                                    expanded = expandedCampusDropdown,
                                    onDismissRequest = { expandedCampusDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Campuses") },
                                        onClick = {
                                            selectedCampus = null
                                            expandedCampusDropdown = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    campuses.forEach { campus ->
                                        DropdownMenuItem(
                                            text = { Text(campus) },
                                            onClick = {
                                                selectedCampus = campus
                                                expandedCampusDropdown = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Role Filter
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedRole?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "All Roles",
                                    onValueChange = { },
                                    label = { Text("Role") },
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Expand role dropdown",
                                            modifier = Modifier.clickable { expandedRoleDropdown = !expandedRoleDropdown }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedRoleDropdown = !expandedRoleDropdown }
                                )
                                DropdownMenu(
                                    expanded = expandedRoleDropdown,
                                    onDismissRequest = { expandedRoleDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Roles") },
                                        onClick = {
                                            selectedRole = null
                                            expandedRoleDropdown = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    roles.forEach { role ->
                                        DropdownMenuItem(
                                            text = { Text(role.replaceFirstChar { it.titlecase(Locale.getDefault()) }) },
                                            onClick = {
                                                selectedRole = role
                                                expandedRoleDropdown = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // User List
                        if (isLoading && users.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (error != null) {
                            Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        } else if (users.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No users found.")
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(users, key = { user -> user.id }) { user ->
                                    UserItem(
                                        user = user,
                                        onClick = {
                                            userToView = user
                                            showUserDetailsDialog = true
                                        },
                                        onEditClick = {
                                            adminViewModel.setSelectedUser(user)
                                            showUserDialog = true
                                        },
                                        onDeleteClick = {
                                            userToDelete = user
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add User Button
                    IconButton(
                        onClick = {
                            adminViewModel.clearSelectedUser()
                            showUserDialog = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add User",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    // User Edit Dialog
    if (showUserDialog) {
        UserEditDialog(
            user = selectedUser,
            onDismiss = { showUserDialog = false },
            onSave = { updatedUserMap, userId ->
                if (userId != null && userId.isNotBlank()) {
                    adminViewModel.updateUser(userId, updatedUserMap) { success, message ->
                        Toast.makeText(context, message ?: (if (success) "User updated" else "Update failed"), Toast.LENGTH_SHORT).show()
                        if (success) showUserDialog = false
                    }
                } else {
                    adminViewModel.createUser(
                        email = updatedUserMap["email"] as String,
                        name = updatedUserMap["name"] as String,
                        phone = updatedUserMap["phone"] as String,
                        campusId = updatedUserMap["campusId"] as String,
                        role = updatedUserMap["role"] as String
                    ) { success, message ->
                        Toast.makeText(context, message ?: (if (success) "User created" else "Creation failed"), Toast.LENGTH_SHORT).show()
                        if (success) showUserDialog = false
                    }
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && userToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete user ${userToDelete?.email ?: ""}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let {
                            adminViewModel.deleteUser(it.id) { success, message ->
                                Toast.makeText(context, message ?: (if (success) "User deleted" else "Deletion failed"), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteDialog = false
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // User Details Dialog
    if (showUserDetailsDialog && userToView != null) {
        AlertDialog(
            onDismissRequest = { showUserDetailsDialog = false },
            title = { Text("User Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${userToView?.name ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Email: ${userToView?.email ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Phone: ${userToView?.phone ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Campus ID: ${userToView?.campusId ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Role: ${userToView?.role?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    userToView?.createdAt?.let {
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        Text("Joined: ${sdf.format(it.toDate())}", style = MaterialTheme.typography.bodyMedium)
                    }
                    userToView?.updatedAt?.let {
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        Text("Last Updated: ${sdf.format(it.toDate())}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showUserDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun UserItem(
    user: User,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Role: ${user.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Campus ID: ${user.campusId}", style = MaterialTheme.typography.bodySmall)
                user.createdAt?.let {
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    Text("Joined: ${sdf.format(it.toDate())}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun UserEditDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>, String?) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var campusId by remember { mutableStateOf(user?.campusId ?: "") }
    var role by remember { mutableStateOf(user?.role ?: "student") }
    var expandedCampusDropdown by remember { mutableStateOf(false) }
    var expandedRoleDropdown by remember { mutableStateOf(false) }

    val campuses = listOf("Ritson", "Steve Biko", "ML Sultan")
    val roles = listOf("student", "security", "admin")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Add New User" else "Edit User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    readOnly = user != null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = campusId,
                        onValueChange = { },
                        label = { Text("Campus ID") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Expand campus dropdown",
                                modifier = Modifier.clickable { expandedCampusDropdown = !expandedCampusDropdown }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCampusDropdown = !expandedCampusDropdown }
                    )
                    DropdownMenu(
                        expanded = expandedCampusDropdown,
                        onDismissRequest = { expandedCampusDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        campuses.forEach { campus ->
                            DropdownMenuItem(
                                text = { Text(campus) },
                                onClick = {
                                    campusId = campus
                                    expandedCampusDropdown = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        onValueChange = { },
                        label = { Text("Role") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Expand role dropdown",
                                modifier = Modifier.clickable { expandedRoleDropdown = !expandedRoleDropdown }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedRoleDropdown = !expandedRoleDropdown }
                    )
                    DropdownMenu(
                        expanded = expandedRoleDropdown,
                        onDismissRequest = { expandedRoleDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        roles.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                                onClick = {
                                    role = selectionOption
                                    expandedRoleDropdown = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val userData = mutableMapOf<String, Any>(
                    "name" to name,
                    "phone" to phone,
                    "campusId" to campusId,
                    "role" to role
                )
                if (user == null) {
                    userData["email"] = email
                }
                if (user != null) {
                    userData["updatedAt"] = Timestamp.now()
                }
                onSave(userData, user?.id)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
