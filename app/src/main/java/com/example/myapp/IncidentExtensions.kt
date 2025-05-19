package com.example.myapp

import com.example.myapp.Incident
import java.text.SimpleDateFormat
import java.util.*

fun Incident.toUiModel(): IncidentUiModel {
    val timeAgo = createdAt?.toDate()?.let {
        getTimeAgoString(it)
    } ?: "Just now"

    return IncidentUiModel(
        id = id,
        title = when (type) {
            "medical" -> "Medical Emergency"
            "theft" -> "Theft Reported"
            "vandalism" -> "Vandalism"
            "suspicious_activity" -> "Suspicious Activity"
            else -> type.replace("_", " ").capitalize()
        },
        type = when {
            type == "medical" -> IncidentType.EMERGENCY
            status == "resolved" -> IncidentType.NOTICE
            else -> IncidentType.WARNING
        },
        timeAgo = timeAgo,
        isEmergency = type == "medical" || status == "in_progress",
        description = description,
        status = status
    )
}

private fun getTimeAgoString(date: Date): String {
    val seconds = (System.currentTimeMillis() - date.time) / 1000
    return when {
        seconds < 60 -> "${seconds.toInt()} seconds ago"
        seconds < 3600 -> "${(seconds / 60).toInt()} minutes ago"
        seconds < 86400 -> "${(seconds / 3600).toInt()} hours ago"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
    }
}

data class IncidentUiModel(
    val id: String,
    val title: String,
    val type: IncidentType,
    val timeAgo: String,
    val isEmergency: Boolean,
    val description: String,
    val status: String
)

enum class IncidentType { EMERGENCY, WARNING, NOTICE }