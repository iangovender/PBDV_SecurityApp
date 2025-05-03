package com.example.myapp

import com.google.firebase.Timestamp

data class Notification(
    val campusId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val targetRole: String = "",
    val createdAt: Timestamp? = null
)