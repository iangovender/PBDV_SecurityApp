package com.example.myapp.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp.LocationData
import com.example.myapp.LocationUtils
import com.example.myapp.LocationViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import androidx.compose.ui.res.painterResource
import com.example.myapp.R
import androidx.core.content.ContextCompat
import android.graphics.drawable.VectorDrawable
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun MapScreen(
    alertId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LocationViewModel = viewModel()
    val locationUtils = LocationUtils(context)
    viewModel.location.value
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var alertLocation by remember { mutableStateOf<LocationData?>(null) }

    // Function to convert vector drawable to bitmap
    fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) as VectorDrawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // Request location permissions
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                locationUtils.requestLocationUpdates(viewModel)
            } else {
                Toast.makeText(
                    context,
                    "Location permission is required to show your location on the map",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    )

    // Request permissions when the screen is first displayed
    LaunchedEffect(Unit) {
        if (!locationUtils.hasLocationPermission(context)) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            locationUtils.requestLocationUpdates(viewModel)
        }
    }

    // Fetch alert location from Firestore
    LaunchedEffect(alertId) {
        val db = Firebase.firestore
        db.collection("sos_alerts")
            .document(alertId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val locationData = document.get("location") as? Map<String, Any>
                    val latitude = locationData?.get("latitude") as? Double
                    val longitude = locationData?.get("longitude") as? Double
                    if (latitude != null && longitude != null) {
                        alertLocation = LocationData(latitude, longitude)
                    } else {
                        mapLoadError = "Invalid location data"
                    }
                } else {
                    mapLoadError = "Alert not found"
                }
            }
            .addOnFailureListener { e ->
                mapLoadError = "Error fetching alert: ${e.message}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "SOS Alert Location",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(48.dp)) // To balance the back button
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            if (mapLoadError != null) {
                Text(
                    text = "Failed to load map: $mapLoadError",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val defaultLocation = LatLng(-29.8518, 31.0078) // Default to DUT coordinates
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        alertLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation,
                        15f
                    )
                }

                // Watch for location changes and update camera position
                LaunchedEffect(alertLocation) {
                    alertLocation?.let {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(
                                    LatLng(it.latitude, it.longitude),
                                    15f
                                ),
                                durationMs = 1000
                            )
                        }
                    }
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        zoomGesturesEnabled = true,
                        scrollGesturesEnabled = true,
                        rotationGesturesEnabled = true,
                        myLocationButtonEnabled = true
                    ),
                    onMyLocationButtonClick = {
                        alertLocation?.let {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        LatLng(it.latitude, it.longitude),
                                        15f
                                    ),
                                    durationMs = 1000
                                )
                            }
                        }
                        true
                    }
                ) {
                    alertLocation?.let {
                        val markerBitmap = getBitmapFromVectorDrawable(R.drawable.ic_student_marker)
                        Marker(
                            state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                            title = "SOS Alert Location",
                            snippet = "Student reported an emergency here",
                            icon = BitmapDescriptorFactory.fromBitmap(markerBitmap)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        alertLocation?.let {
            Text(
                text = "Latitude: ${it.latitude}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Longitude: ${it.longitude}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Address: ${locationUtils.reverseGeocodeLocation(it)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        } ?: Text(
            text = "Waiting for location...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 