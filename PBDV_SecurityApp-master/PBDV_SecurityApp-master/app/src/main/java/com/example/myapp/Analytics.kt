package com.example.myapp

import com.google.firebase.Timestamp

data class Analytics(
    val incidentCounts: Map<String, Int> = emptyMap(),
    val totalIncidents: Int = 0,
    val sosAlerts: Int = 0,
    val lastUpdated: Timestamp? = null
)