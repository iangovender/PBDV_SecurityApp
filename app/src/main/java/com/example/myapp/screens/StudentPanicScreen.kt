package com.example.myapp.screens



import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

@Composable
fun StudentPanicScreen(onAlertSent: (String) -> Unit) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(10) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showDialog = true
                coroutineScope.launch {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(context, "Please enable GPS for accurate location", Toast.LENGTH_LONG).show()
                        locationError = "GPS is disabled. Using last known location."
                    }

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    try {
                        val lastLocation = fusedLocationClient.lastLocation.await()
                        if (lastLocation != null) {
                            currentLocation = lastLocation
                            Toast.makeText(context, "Location captured: Lat ${lastLocation.latitude}, Lon ${lastLocation.longitude}", Toast.LENGTH_LONG).show()
                        } else {
                            locationError = "Unable to fetch location. Using default location."
                            Toast.makeText(context, "No location available. Using default DUT coordinates.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        locationError = "Failed to fetch location: ${e.message}"
                        Toast.makeText(context, "Failed to fetch location: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
                locationError = "Location permission denied"
            }
        }
    )

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            if (countdown == 0) {
                val db = Firebase.firestore
                val alert = hashMapOf(
                    "studentId" to "student_123",
                    "campusId" to "dut",
                    "location" to hashMapOf(
                        "latitude" to (currentLocation?.latitude ?: -29.8518),
                        "longitude" to (currentLocation?.longitude ?: 31.0078)
                    ),
                    "status" to "pending",
                    "assignedTo" to null,
                    "createdAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    "updatedAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    "message" to "HELP EMERGENCY"
                )

                db.collection("sos_alerts")
                    .add(alert)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(context, "SOS alert sent!", Toast.LENGTH_SHORT).show()
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
                        onAlertSent(documentReference.id)
                        isCountingDown = false
                        countdown = 30
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to send SOS: ${e.message}", Toast.LENGTH_LONG).show()
                        isCountingDown = false
                        countdown = 30
                    }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Student Panic Button",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        if (locationError != null) {
            Text(
                text = locationError!!,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        if (isCountingDown) {
            Text(
                text = "Security will be notified in $countdown seconds",
                fontSize = 18.sp,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = {
                    isCountingDown = false
                    countdown = 30
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
                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                },
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.Red, CircleShape)
            ) {
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }
    }

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
