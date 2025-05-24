package com.example.myapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Notification(
    @DocumentId val id: String = "", 
    val title: String = "",
    val message: String = "",
    val type: String = "", 
    val targetRole: String = "", 
    val campusId: String = "all_campuses", 
    val createdAt: Timestamp? = Timestamp.now()
)
