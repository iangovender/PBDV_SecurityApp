package com.example.myapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Notification(
    @DocumentId val id: String = "", // Firestore document ID
    val title: String = "",
    val message: String = "",
    val type: String = "", // e.g., "incident", "SOS", "general"
    val targetRole: String = "", // e.g., "student", "security", "all_users"
    val campusId: String = "all_campuses", // Optional: for campus-specific notifications
    val createdAt: Timestamp? = Timestamp.now()
)
