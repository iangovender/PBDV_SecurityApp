package com.example.myapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.myapp.LocationData // Added import for LocationData

data class Incident(
    @DocumentId val id: String = "",
    val reportedBy: String = "",
    val campusId: String = "",
    val type: String = "",
    val description: String = "",
    val location: LocationData? = null,
    val images: List<String> = emptyList(),
    var status: String = "Active",
    val assignedTo: String = "",
    val createdAt: Timestamp? = Timestamp.now(),
    var updatedAt: Timestamp? = Timestamp.now()
)