package com.example.myapp // Ensure this package is correct

import com.google.firebase.Timestamp

data class User(
    val id: String = "", // Firestore document ID, populated by your ViewModel when fetching
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val campusId: String = "",
    val role: String = "student", // Default role
    val fcmToken: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null // For tracking user document updates
)
