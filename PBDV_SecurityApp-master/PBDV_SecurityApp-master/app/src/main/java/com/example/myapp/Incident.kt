package com.example.myapp

import com.google.firebase.Timestamp

data class Incident(
    val reportedBy: String = "",
    val campusId: String = "",
    val type: String = "",
    val description: String = "",
    val location: LocationData = LocationData(0.0, 0.0),
    val images: List<String> = emptyList(),
    val status: String = "active",
    val assignedTo: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)