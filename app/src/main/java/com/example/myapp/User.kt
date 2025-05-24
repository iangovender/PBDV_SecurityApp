package com.example.myapp 

import com.google.firebase.Timestamp

data class User(
    val id: String = "", 
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val campusId: String = "",
    val role: String = "student", 
    val fcmToken: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null 
)
