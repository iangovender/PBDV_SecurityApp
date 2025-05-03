package com.example.myapp

import com.google.firebase.Timestamp

data class SosAlert(
    val studentId: String = "",
    val campusId: String = "",
    val location: LocationData = LocationData(0.0, 0.0),
    val message: String = "",
    val status: String = "pending",
    val assignedTo: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)