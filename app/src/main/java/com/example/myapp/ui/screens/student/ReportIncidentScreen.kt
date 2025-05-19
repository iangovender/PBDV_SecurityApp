package com.example.myapp.ui.screens.student

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.Incident
import com.example.myapp.LocationData
import com.example.myapp.User
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    onSubmit: () -> Unit,
    navController: NavController
) {
    var campusId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val db = Firebase.firestore
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Form states
    var type by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<Location?>(null) }

    // UI states
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Dropdown states
    var isTypeExpanded by remember { mutableStateOf(false) }

    // Options
    val incidentTypes = listOf("Accident", "Theft", "Vandalism", "Medical", "Security", "Other")

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val userSnapshot = db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                val user = userSnapshot.toObject(User::class.java)
                campusId = user?.campusId?.takeIf { it.isNotEmpty() }
                if (campusId == null) {
                    error = "Campus ID not set for user"
                }
            } catch (e: Exception) {
                error = "Error fetching user data: ${e.message}"
            }
        } else {
            error = "User not logged in"
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fetchLocation(context) { loc ->
                    location = loc
                    Toast.makeText(context, "Location captured", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Report Incident",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Incident Type Dropdown
        ExposedDropdownMenuBox(
            expanded = isTypeExpanded,
            onExpandedChange = { isTypeExpanded = it }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text("Incident Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = isTypeExpanded,
                onDismissRequest = { isTypeExpanded = false }
            ) {
                incidentTypes.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            type = selectionOption
                            isTypeExpanded = false
                        }
                    )
                }
            }
        }

        // Description Field
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            placeholder = { Text("Describe the incident in detail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 5
        )

        // Location Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Current Location")
            }
        }

        if (location != null) {
            Text(
                text = "Location: ${location?.latitude}, ${location?.longitude}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Error Message
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Submit Button
        Button(
            onClick = {
                if (validateForm(type, description, location, campusId, error)) {
                    isLoading = true
                    submitIncident(
                        type = type,
                        description = description,
                        location = location,
                        campusId = campusId!!,
                        reportedBy = currentUser?.uid ?: "",
                        db = db,
                        onSuccess = {
                            isLoading = false
                            onSubmit()
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("refresh", true)

                            navController.popBackStack()
                        },
                        onError = { e ->
                            isLoading = false
                            error = "Failed to submit incident: ${e.message}"
                        }
                    )
                } else {
                    error = "Please fill all required fields and ensure location is captured"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Submit",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun validateForm(
    type: String,
    description: String,
    location: Location?,
    campusId: String?,
    error: String?
): Boolean {
    return type.isNotEmpty() &&
            description.isNotEmpty() &&
            location != null &&
            campusId != null &&
            error == null
}

private fun submitIncident(
    type: String,
    description: String,
    location: Location?,
    campusId: String,
    reportedBy: String,
    db: com.google.firebase.firestore.FirebaseFirestore,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val locationData = location?.let {
            LocationData(
                latitude = it.latitude,
                longitude = it.longitude
            )
        }

        val incident = Incident(
            reportedBy = reportedBy,
            campusId = campusId,
            type = type,
            description = description,
            location = locationData,
            status = "Active",
            assignedTo = ""
        )

        db.collection("incidents")
            .add(incident)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    } catch (e: Exception) {
        onError(e)
    }
}

private fun fetchLocation(context: Context, onLocationFetched: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            onLocationFetched(location)
        }
        .addOnFailureListener {
            onLocationFetched(null)
        }
}