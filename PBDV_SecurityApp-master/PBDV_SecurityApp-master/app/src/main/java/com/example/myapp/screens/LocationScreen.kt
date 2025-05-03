package com.example.myapp.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.NavController

@Composable
fun LocationScreen(navController: NavController? = null) {
    val viewModel: LocationViewModel = viewModel()
    val context = LocalContext.current
    val locationUtils = LocationUtils(context)
    val location = viewModel.location.value
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                locationUtils.requestLocationUpdates(viewModel = viewModel)
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

    // Save SOS alert and send notification
    suspend fun saveSosAlert() {
        if (location == null || currentUser == null) {
            Toast.makeText(context, "Location: $location, User: $currentUser", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Fetch user data from Firestore
            val userSnapshot = db.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            if (!userSnapshot.exists()) {
                Toast.makeText(context, "User data not found in Firestore!", Toast.LENGTH_LONG).show()
                return
            }

            val user = userSnapshot.toObject(User::class.java)
            if (user == null) {
                Toast.makeText(context, "Unable to fetch user data", Toast.LENGTH_SHORT).show()
                return
            }

            // Check the user's role
            val userRole = user.role ?: ""
            Toast.makeText(context, "Your role is: $userRole", Toast.LENGTH_SHORT).show()
            if (userRole !in listOf("student", "security", "admin")) {
                Toast.makeText(context, "Your role ($userRole) can't send SOS alerts!", Toast.LENGTH_LONG).show()
                return
            }

            // Use campusId from user data
            val campusId = user.campusId.takeIf { it.isNotEmpty() } ?: run {
                Toast.makeText(context, "Campus ID not set for user", Toast.LENGTH_SHORT).show()
                return
            }

            // Create SOS alert
            val sosAlert = SosAlert(
                studentId = currentUser.uid,
                campusId = campusId,
                location = LocationData(location.latitude, location.longitude),
                message = "Emergency alert from student",
                status = "pending",
                assignedTo = "",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // Save to Firestore
            db.collection("sosAlerts").add(sosAlert).await()

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
            // Note: Actual FCM message sending would typically be handled server-side
            // This is a simplified version
            Toast.makeText(context, "SOS alert sent successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error sending SOS alert: ${e.message}", Toast.LENGTH_LONG).show()
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
        
        // Add Map View button
        Button(
            onClick = { navController?.navigate("map") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("View Map")
        }

        Button(
            onClick = {
                if (!locationUtils.isLocationEnabled()) {
                    Toast.makeText(context, "Please turn on location services!", Toast.LENGTH_LONG)
                        .show()
                    return@Button
                }
                if (locationUtils.hasLocationPermission(context)) {
                    locationUtils.requestLocationUpdates(viewModel)
                    // Launch coroutine to save SOS alert
                    CoroutineScope(Dispatchers.Main).launch {
                        var attempts = 0
                        while (viewModel.location.value == null && attempts < 20) {
                            delay(500)
                            attempts++
                        }
                        if (viewModel.location.value != null) {
                            saveSosAlert()
                        } else {
                            Toast.makeText(
                                context,
                                "Couldn't find your location. Try again!",
                                Toast.LENGTH_LONG
                            ).show()
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