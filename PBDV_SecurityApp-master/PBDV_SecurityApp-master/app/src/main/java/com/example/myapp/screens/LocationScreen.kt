package com.example.myapp.screens

import android.Manifest
import android.content.Context
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationScreen(
    onAlertSent: (String) -> Unit = {} // Callback to navigate to CommunicationScreen
) {
    val viewModel: LocationViewModel = viewModel()
    val context = LocalContext.current
    val locationUtils = LocationUtils(context)
    val location = viewModel.location.value
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // State for countdown and dialog
    var showDialog by remember { mutableStateOf(false) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(10) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                locationUtils.requestLocationUpdates(viewModel)
            } else {
                val rationalRequired = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    context as MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (rationalRequired) {
                    Toast.makeText(
                        context,
                        "Location Permission is required for this app",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Location Permission is required. Please enable it in Android settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    )

    // Handle countdown logic
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            if (countdown == 0) {
                // Proceed to save SOS alert when countdown finishes
                if (location == null || currentUser == null) {
                    Toast.makeText(context, "Location: $location, User: $currentUser", Toast.LENGTH_LONG).show()
                    isCountingDown = false
                    countdown = 10
                    return@LaunchedEffect
                }

                try {
                    // Fetch user data from Firestore
                    val userSnapshot = db.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (!userSnapshot.exists()) {
                        Toast.makeText(context, "User data not found in Firestore!", Toast.LENGTH_LONG).show()
                        isCountingDown = false
                        countdown = 10
                        return@LaunchedEffect
                    }

                    val user = userSnapshot.toObject(User::class.java)
                    if (user == null) {
                        Toast.makeText(context, "Unable to fetch user data", Toast.LENGTH_SHORT).show()
                        isCountingDown = false
                        countdown = 10
                        return@LaunchedEffect
                    }

                    // Check the user's role
                    val userRole = user.role ?: ""
                    Toast.makeText(context, "Your role is: $userRole", Toast.LENGTH_SHORT).show()
                    if (userRole !in listOf("student", "security", "admin")) {
                        Toast.makeText(context, "Your role ($userRole) can’t send SOS alerts!", Toast.LENGTH_LONG).show()
                        isCountingDown = false
                        countdown = 10
                        return@LaunchedEffect
                    }

                    // Use campusId from user data
                    val campusId = user.campusId.takeIf { it.isNotEmpty() } ?: run {
                        Toast.makeText(context, "Campus ID not set for user", Toast.LENGTH_SHORT).show()
                        isCountingDown = false
                        countdown = 10
                        return@LaunchedEffect
                    }

                    // Create SOS alert using the SosAlert model
                    val sosAlert = SosAlert(
                        studentId = currentUser.uid,
                        campusId = campusId,
                        location = LocationData(latitude = location.latitude, longitude = location.longitude),
                        message = "Emergency alert from student",
                        status = "pending",
                        assignedTo = "",
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )

                    // Save to Firestore and get the document reference
                    val documentReference = db.collection("sos_alerts").add(sosAlert).await()

                    // Add initial message to messages subcollection
                    val autoMessage = hashMapOf(
                        "sender" to "student",
                        "text" to "HELP EMERGENCY",
                        "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                        "type" to "text",
                        "delivered" to false
                    )
                    db.collection("sos_alerts")
                        .document(documentReference.id)
                        .collection("messages")
                        .add(autoMessage)
                        .await()

                    // Create notification for security
                    val notification = Notification(
                        campusId = campusId,
                        title = "Emergency Alert",
                        message = "New SOS alert from student at ${location.latitude}, ${location.longitude}",
                        type = "sos",
                        targetRole = "security",
                        createdAt = Timestamp.now()
                    )

                    // Save notification
                    db.collection("notifications").add(notification).await()

                    // Send FCM notification to security
                    val securityTopic = "security_team"
                    FirebaseMessaging.getInstance().subscribeToTopic(securityTopic)
                    Toast.makeText(context, "SOS alert sent successfully", Toast.LENGTH_LONG).show()

                    // Navigate to CommunicationScreen with the alertId
                    onAlertSent(documentReference.id)

                    // Reset countdown state
                    isCountingDown = false
                    countdown = 10
                } catch (e: Exception) {
                    Toast.makeText(context, "Error sending SOS alert: ${e.message}", Toast.LENGTH_LONG).show()
                    isCountingDown = false
                    countdown = 10
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Press the button to send an emergency alert",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Current Location: ${location?.latitude ?: "Unknown"}, ${location?.longitude ?: "Unknown"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isCountingDown) {
            Text(
                text = "Security will be notified in $countdown seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = {
                    isCountingDown = false
                    countdown = 10
                    Toast.makeText(context, "SOS alert canceled", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Cancel SOS Alert")
            }
        } else {
            Button(
                onClick = {
                    if (!locationUtils.isLocationEnabled()) {
                        Toast.makeText(context, "Please turn on location services!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (locationUtils.hasLocationPermission(context)) {
                        locationUtils.requestLocationUpdates(viewModel)
                        CoroutineScope(Dispatchers.Main).launch {
                            var attempts = 0
                            while (viewModel.location.value == null && attempts < 20) {
                                delay(500)
                                attempts++
                            }
                            if (viewModel.location.value != null) {
                                showDialog = true // Show confirmation dialog
                            } else {
                                Toast.makeText(context, "Couldn’t find your location. Try again!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Emergency Alert")
            }
        }
    }

    // Confirmation dialog before starting countdown
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Send SOS Alert") },
            text = { Text("Are you sure you want to send an SOS alert?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        isCountingDown = true
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun ReportIncidentScreen(
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val db = Firebase.firestore

    var incidentType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<Location?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Report an Incident",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Incident Type Dropdown
        OutlinedTextField(
            value = incidentType,
            onValueChange = { incidentType = it },
            label = { Text("Incident Type") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description Text Area
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (location == null) {
            Button(onClick = {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }) {
                Text("Fetch Location")
            }
        } else {
            Text("Location: Lat ${location!!.latitude}, Lon ${location!!.longitude}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = {
                if (incidentType.isBlank() || description.isBlank()) {
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSubmitting = true

                // Create incident data
                val incidentData = hashMapOf(
                    "incidentType" to incidentType,
                    "description" to description,
                    "location" to hashMapOf(
                        "latitude" to (location?.latitude ?: 0.0),
                        "longitude" to (location?.longitude ?: 0.0)
                    ),
                    "createdAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )

                // Save to Firestore
                db.collection("incidents")
                    .add(incidentData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Incident reported successfully", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        onSubmit()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to report incident: ${e.message}", Toast.LENGTH_LONG).show()
                        isSubmitting = false
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            Text(if (isSubmitting) "Submitting..." else "Submit Incident")
        }
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