package com.example.myapp

data class Campus(
    val name: String = "",
    val location: LocationData = LocationData(0.0, 0.0),
    val radius: Double = 0.0
)