package com.example.myapp

import android.util.Log
import com.example.myapp.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.ktx.toObject

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")
    private val functions = FirebaseFunctions.getInstance()

    companion object {
        private const val TAG = "NotificationRepository"
    }

    // Fetches all notifications ordered by creation date
    fun getAllNotifications(): Flow<List<Notification>> {
        val flow = MutableStateFlow<List<Notification>>(emptyList())
        notificationsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    flow.value = emptyList() // Or handle error appropriately
                    return@addSnapshotListener
                }
                snapshot?.let {
                    flow.value = it.toObjects()
                }
            }
        return flow.asStateFlow()
    }

    // Creates a new notification in Firestore
    suspend fun createNotification(notification: Notification): Result<Unit> {
        return try {
            notificationsCollection.add(notification).await()
            Log.d(TAG, "Notification successfully created in Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification in Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Sends an FCM notification.
     * This function calls a Firebase Cloud Function named 'sendFcmNotification'.
     * The Cloud Function is responsible for the actual FCM sending logic using the Admin SDK.
     *
     * @param title The title of the notification.
     * @param message The body/message of the notification.
     * @param targetRole The role of users to target (e.g., "student", "security_team").
     * The Cloud Function should map this role to a topic or specific tokens.
     * @return A Result indicating success or failure.
     */
    suspend fun sendFcmNotification(title: String, message: String, targetRole: String): Result<String> {
        val data = hashMapOf(
            "title" to title,
            "message" to message,
            "targetRole" to targetRole // Your Cloud Function will use this to determine the FCM topic or tokens
        )

        return try {
            val result = functions
                .getHttpsCallable("sendFcmNotification") // Ensure this matches your Cloud Function name
                .call(data)
                .await()
            val responseMessage = result.data as? String ?: "Successfully triggered FCM, but no specific message from function."
            Log.d(TAG, "FCM function called successfully: $responseMessage")
            Result.success(responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling FCM function", e)
            Result.failure(e)
        }
    }
}
