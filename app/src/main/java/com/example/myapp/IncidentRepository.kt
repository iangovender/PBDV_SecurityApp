package com.example.myapp

import android.util.Log
import com.example.myapp.Incident
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp // Ensure Timestamp is imported

class IncidentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val incidentsCollection = db.collection("incidents")

    companion object {
        private const val TAG = "IncidentRepository"
    }

    /**
     * Fetches incidents with real-time updates, allowing for optional filtering.
     * @param campusFilter Optional campus ID to filter by.
     * @param statusFilter Optional status to filter by.
     * @return A Flow emitting a list of incidents.
     */
    fun getIncidents(
        campusFilter: String? = null,
        statusFilter: String? = null
        // Search query will be applied client-side in ViewModel for simplicity with Firestore listeners
    ): Flow<List<Incident>> {
        val incidentsFlow = MutableStateFlow<List<Incident>>(emptyList())

        var query: Query = incidentsCollection.orderBy("createdAt", Query.Direction.DESCENDING)

        if (!campusFilter.isNullOrBlank() && campusFilter != "All Campuses") {
            query = query.whereEqualTo("campusId", campusFilter)
        }
        if (!statusFilter.isNullOrBlank() && statusFilter != "All Statuses") {
            query = query.whereEqualTo("status", statusFilter.lowercase()) // Ensure status is stored consistently (e.g., lowercase)
        }

       
        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listen failed for incidents.", error)
                incidentsFlow.value = emptyList() // Or emit an error state
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                try {
                    val incidentList = querySnapshot.toObjects<Incident>()
                    incidentsFlow.value = incidentList
                    Log.d(TAG, "Fetched ${incidentList.size} incidents. Campus: $campusFilter, Status: $statusFilter")
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting incidents", e)
                    incidentsFlow.value = emptyList()
                }
            }
        }
        
        return incidentsFlow.asStateFlow()
    }

    /**
     * Updates the status of a specific incident.
     * @param incidentId The ID of the incident to update.
     * @param newStatus The new status to set.
     * @return Result indicating success or failure.
     */
    suspend fun updateIncidentStatus(incidentId: String, newStatus: String): Result<Unit> {
        if (incidentId.isBlank()) {
            Log.e(TAG, "Incident ID cannot be blank for status update.")
            return Result.failure(IllegalArgumentException("Incident ID is blank."))
        }
        return try {
            incidentsCollection.document(incidentId)
                .update(mapOf(
                    "status" to newStatus.lowercase(), // Store status consistently
                    "updatedAt" to Timestamp.now()
                ))
                .await()
            Log.d(TAG, "Incident $incidentId status updated to $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating incident $incidentId status", e)
            Result.failure(e)
        }
    }

    
}
