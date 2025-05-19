package com.example.myapp.screens

//package com.example.campussecuritysystem.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone

@Composable
fun CommunicationScreen(alertId: String, sender: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var newMessage by remember { mutableStateOf("") }
    var alertData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    var isOtherTyping by remember { mutableStateOf(false) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Call permission denied. Please grant permission to make calls.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(alertId) {
        val db = Firebase.firestore
        db.collection("sos_alerts")
            .document(alertId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error fetching alert: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    data?.put("id", snapshot.id)
                    alertData = data
                }
            }

        db.collection("sos_alerts")
            .document(alertId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error fetching messages: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messageList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        data?.put("id", doc.id)
                        data
                    }
                    messages = messageList
                }
            }

        val otherSender = if (sender == "student") "security" else "student"
        db.collection("sos_alerts")
            .document(alertId)
            .collection("typing")
            .document(otherSender)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                isOtherTyping = snapshot?.getBoolean("isTyping") == true
            }
    }

    LaunchedEffect(newMessage) {
        val db = Firebase.firestore
        val typingData = hashMapOf(
            "isTyping" to (newMessage.isNotBlank())
        )
        db.collection("sos_alerts")
            .document(alertId)
            .collection("typing")
            .document(sender)
            .set(typingData)
    }

    val location = alertData?.get("location") as? Map<String, Any>
    val latitude = location?.get("latitude") as? Double ?: -29.8518
    val longitude = location?.get("longitude") as? Double ?: 31.0078
    val alertLocation = LatLng(latitude, longitude)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (sender == "student") Color(0xFFF0F8FF) else Color(0xFFFFF0F0))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (sender == "student") "Communicate with Security" else "Communicate with Student",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You are ${if (sender == "student") "a Student" else "Security"}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
//                Button(
//                    onClick = onBack
//                ) {
//                    Text("Back")
//                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
            ) {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(alertLocation, 15f)
                }

                if (mapLoadError != null) {
                    Text(
                        text = "Failed to load map: $mapLoadError",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            zoomGesturesEnabled = true,
                            scrollGesturesEnabled = true
                        )
                    ) {
                        if (latitude != -29.8518 || longitude != 31.0078) {
                            Marker(
                                state = MarkerState(position = alertLocation),
                                title = "Alert Location",
                                snippet = "Student reported an emergency here"
                            )
                        }
                    }
                    if (sender == "security") {
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to open Google Maps: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text("Get Directions")
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
                reverseLayout = true
            ) {
                items(messages) { message: Map<String, Any> ->
                    val messageSender = message["sender"] as? String ?: "Unknown"
                    val text = message["text"] as? String ?: ""
                    val timestamp = message["timestamp"] as? String ?: ""
                    val messageType = message["type"] as? String ?: "text"
                    val isDelivered = message["delivered"] as? Boolean == true
                    val shortTimestamp = try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val date = inputFormat.parse(timestamp)
                        outputFormat.format(date)
                    } catch (e: Exception) {
                        "N/A"
                    }
                    val isStudentMessage = messageSender == "student"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isStudentMessage) Arrangement.Start else Arrangement.End
                    ) {
                        Column(
                            horizontalAlignment = if (isStudentMessage) Alignment.Start else Alignment.End,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isStudentMessage) Color(0xFF90EE90) else Color(0xFFFFFFFF)
                                ),
                                shape = if (isStudentMessage) RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp)
                                else RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier
                                    .padding(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (messageType == "call") {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call Icon",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Voice Call",
                                            fontSize = 16.sp
                                        )
                                    } else {
                                        Text(
                                            text = text,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = shortTimestamp,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                )
                                if (messageType == "text" && messageSender == sender && isDelivered) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delivered",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isOtherTyping) {
                Text(
                    text = "${if (sender == "student") "Security" else "Student"} is typing...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                label = { Text("Type a message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Button(
                onClick = {
                    if (newMessage.isNotBlank()) {
                        val db = Firebase.firestore
                        val message = hashMapOf(
                            "sender" to sender,
                            "text" to newMessage,
                            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            "type" to "text",
                            "delivered" to false
                        )
                        db.collection("sos_alerts")
                            .document(alertId)
                            .collection("messages")
                            .add(message)
                            .addOnSuccessListener {
                                newMessage = ""
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            ) {
                Text("Send")
            }
        }

        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    callPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
                } else {
                    val phoneNumber = if (sender == "student") "tel:+0799604456" else "tel:+0738228359"
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse(phoneNumber)
                    }
                    try {
                        context.startActivity(intent)
                        val db = Firebase.firestore
                        val callMessage = hashMapOf(
                            "sender" to sender,
                            "text" to "Voice Call",
                            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            "type" to "call"
                        )
                        db.collection("sos_alerts")
                            .document(alertId)
                            .collection("messages")
                            .add(callMessage)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to make call: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(if (sender == "student") "Call Security" else "Call Student")
        }
    }
}