// File: app/src/main/java/com/example/myapp/AdminViewModel.kt
package com.example.myapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.Incident // Import Incident data class
import com.example.myapp.Notification
import com.example.myapp.User
import com.example.myapp.SosAlert
import com.example.myapp.IncidentRepository // Import IncidentRepository
import com.example.myapp.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AdminDashboardAnalytics(
    val totalUsers: Int = 0,
    val studentUsers: Int = 0,
    val securityUsers: Int = 0,
    val totalSosAlerts: Int = 0,
    val pendingSosAlerts: Int = 0,
    val resolvedSosAlerts: Int = 0,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Any? = null
) {
    fun getUpdatedAtTimestamp(): Timestamp? {
        return when (updatedAt) {
            is Timestamp -> updatedAt as Timestamp
            is String -> try {
                // Parse ISO 8601 string to Timestamp
                val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .parse(updatedAt as String)
                Timestamp(date)
            } catch (e: Exception) {
                Timestamp.now()
            }
            else -> Timestamp.now()
        }
    }

    fun hasValidTimestamp(): Boolean {
        return when (updatedAt) {
            null -> true // Consider if null should be valid in your case
            is Timestamp -> true
            is String -> try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .parse(updatedAt as String)
                true
            } catch (e: Exception) {
                false
            }
            else -> false
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class) // For Flow transformations like combine
class AdminViewModel : ViewModel() {
    private val repository = AdminRepository()
    private val db = FirebaseFirestore.getInstance()
    private val notificationRepository = NotificationRepository()
    private val incidentRepository = IncidentRepository() // Instantiate IncidentRepository

    // Analytics StateFlow
    private val _analytics = MutableStateFlow(AdminDashboardAnalytics())
    val analytics: StateFlow<AdminDashboardAnalytics> = _analytics.asStateFlow()

    // Users List StateFlow
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Selected user for editing
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    // --- Notifications StateFlows ---
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isNotificationsLoading = MutableStateFlow(false)
    val isNotificationsLoading: StateFlow<Boolean> = _isNotificationsLoading.asStateFlow()

    private val _notificationError = MutableStateFlow<String?>(null)
    val notificationError: StateFlow<String?> = _notificationError.asStateFlow()

    // --- Incident Management StateFlows ---
    private val _rawIncidents = MutableStateFlow<List<Incident>>(emptyList()) // Raw list from Firestore

    val incidentSearchQuery = MutableStateFlow("")
    val incidentCampusFilter = MutableStateFlow<String?>(null) // e.g., "All Campuses" or specific ID
    val incidentStatusFilter = MutableStateFlow<String?>(null) // e.g., "All Statuses", "active", "resolved"

    private val _isIncidentsLoading = MutableStateFlow(false)
    val isIncidentsLoading: StateFlow<Boolean> = _isIncidentsLoading.asStateFlow()

    private val _incidentError = MutableStateFlow<String?>(null)
    val incidentError: StateFlow<String?> = _incidentError.asStateFlow()

    // Combined and filtered incidents list exposed to the UI
    val filteredIncidents: StateFlow<List<Incident>> = combine(
        _rawIncidents,
        incidentSearchQuery,
        // incidentCampusFilter, // Firestore query handles these, but search is client-side
        // incidentStatusFilter
    ) { incidents, query /*, campus, status */ ->
        // Apply search query client-side
        if (query.isBlank()) {
            incidents
        } else {
            incidents.filter { incident ->
                incident.type.contains(query, ignoreCase = true) ||
                        incident.description.contains(query, ignoreCase = true) ||
                        incident.id.contains(query, ignoreCase = true) || // Allow searching by ID
                        incident.reportedBy.contains(query, ignoreCase = true)
            }
        }
        // Campus and status filtering is done by the Firestore query in the repository
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- End of Incident Management StateFlows ---

    // General Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchDashboardAnalytics()
        fetchNotifications()
        // Initial fetch for incidents. Filters can be applied later.
        observeIncidents()
    }

//    fun fetchDashboardAnalytics() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _error.value = null
//            try {
//                val usersSnapshot = db.collection("users").get().await()
//                val allUsers = usersSnapshot.documents.mapNotNull { it.toObject<User>() }
//                val studentCount = allUsers.count { it.role == "student" }
//                val securityCount = allUsers.count { it.role == "security" }
//
//                val alertsSnapshot = db.collection("sos_alerts").get().await()
//                val allAlerts = alertsSnapshot.documents.mapNotNull { it.toObject<SosAlert>() }
//                val pendingAlertsCount = allAlerts.count { it.status == "pending" }
//                val resolvedAlertsCount = allAlerts.count { it.status == "resolved" }
//
//                _analytics.value = AdminDashboardAnalytics(
//                    totalUsers = allUsers.size,
//                    studentUsers = studentCount,
//                    securityUsers = securityCount,
//                    totalSosAlerts = allAlerts.size,
//                    pendingSosAlerts = pendingAlertsCount,
//                    resolvedSosAlerts = resolvedAlertsCount
//                )
//            } catch (e: Exception) {
//                Log.e("AdminViewModel", "Error fetching dashboard analytics", e)
//                _error.value = "Failed to load dashboard data: ${e.message}"
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

    fun fetchDashboardAnalytics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.getDashboardData()

                // ADD THIS VALIDATION to ensure timestamp is properly handled
                if (result.updatedAt != null && result.getUpdatedAtTimestamp() == null) {
                    throw Exception("Invalid timestamp format in updatedAt field")
                }

                _analytics.value = result
                _error.value = null
            } catch (e: Exception) {
                // MODIFY THIS ERROR HANDLING to be more specific
                _error.value = when {
                    e.message?.contains("Failed to convert value of type java.lang.String to Timestamp") == true -> {
                        "Data format error. Please ensure all timestamps are properly formatted."
                    }
                    e.message?.contains("Invalid timestamp format") == true -> {
                        "Invalid timestamp format in database. Contact support."
                    }
                    else -> "Error fetching data: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Notification Management Functions ---
    fun fetchNotifications() {
        viewModelScope.launch {
            _isNotificationsLoading.value = true
            _notificationError.value = null
            notificationRepository.getAllNotifications()
                .catch { e ->
                    Log.e("AdminViewModel", "Error fetching notifications", e)
                    _notificationError.value = "Failed to load notifications: ${e.message}"
                    _isNotificationsLoading.value = false
                }
                .collect { notificationList ->
                    _notifications.value = notificationList
                    _isNotificationsLoading.value = false
                }
        }
    }

    fun createAndSendNotification(
        title: String,
        message: String,
        type: String,
        targetRole: String,
        campusId: String = "all_campuses",
        onResult: (Boolean, String?) -> Unit
    ) {
        if (title.isBlank() || message.isBlank() || type.isBlank() || targetRole.isBlank()) {
            onResult(false, "All fields (Title, Message, Type, Target Role) are required.")
            return
        }
        viewModelScope.launch {
            // Use a specific loading state or general if preferred
            _isNotificationsLoading.value = true
            _notificationError.value = null
            val newNotification = Notification(
                title = title, message = message, type = type,
                targetRole = targetRole, campusId = campusId, createdAt = Timestamp.now()
            )
            val firestoreResult = notificationRepository.createNotification(newNotification)
            if (firestoreResult.isFailure) {
                val errorMsg = "Firestore save failed: ${firestoreResult.exceptionOrNull()?.message}"
                Log.e("AdminViewModel", errorMsg)
                _notificationError.value = errorMsg
                _isNotificationsLoading.value = false
                onResult(false, errorMsg)
                return@launch
            }
            Log.d("AdminViewModel", "Notification saved. Sending FCM.")
            val fcmResult = notificationRepository.sendFcmNotification(
                newNotification.title, newNotification.message, newNotification.targetRole
            )
            _isNotificationsLoading.value = false
            if (fcmResult.isSuccess) {
                val successMsg = "Notification sent: ${fcmResult.getOrNull()}"
                Log.d("AdminViewModel", successMsg)
                onResult(true, successMsg)
            } else {
                val errorMsg = "Saved, but FCM failed: ${fcmResult.exceptionOrNull()?.message}"
                Log.e("AdminViewModel", errorMsg)
                _notificationError.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }

    // --- Incident Management Functions ---

    /**
     * Observes incidents based on current filter values.
     * This function is called initially and whenever filters change.
     */
    fun observeIncidents() {
        viewModelScope.launch {
            _isIncidentsLoading.value = true
            _incidentError.value = null

            // Combine filters to re-trigger observation when any filter changes
            combine(
                incidentCampusFilter,
                incidentStatusFilter
            ) { campus, status ->
                Pair(campus, status)
            }.flatMapLatest { (campus, status) -> // Use flatMapLatest to cancel previous collection when filters change
                incidentRepository.getIncidents(
                    campusFilter = if (campus == "All Campuses") null else campus,
                    statusFilter = if (status == "All Statuses") null else status
                )
            }
                .catch { e ->
                    Log.e("AdminViewModel", "Error observing incidents", e)
                    _incidentError.value = "Failed to load incidents: ${e.message}"
                    _rawIncidents.value = emptyList() // Clear previous data on error
                    _isIncidentsLoading.value = false
                }
                .collect { incidentList ->
                    _rawIncidents.value = incidentList
                    _isIncidentsLoading.value = false
                    _incidentError.value = null // Clear error on successful fetch
                    if (incidentList.isEmpty()) {
                        Log.d("AdminViewModel", "No incidents found for current filters.")
                    }
                }
        }
    }

    /**
     * Updates the search query for incidents.
     */
    fun onIncidentSearchQueryChanged(query: String) {
        incidentSearchQuery.value = query
    }

    /**
     * Updates the campus filter for incidents and triggers re-fetching.
     */
    fun onIncidentCampusFilterChanged(campus: String?) {
        incidentCampusFilter.value = campus
        // observeIncidents() // Re-observing is handled by flatMapLatest on filter change
    }

    /**
     * Updates the status filter for incidents and triggers re-fetching.
     */
    fun onIncidentStatusFilterChanged(status: String?) {
        incidentStatusFilter.value = status
        // observeIncidents() // Re-observing is handled by flatMapLatest on filter change
    }

    /**
     * Updates the status of a given incident.
     * @param incidentId The ID of the incident.
     * @param newStatus The new status to set.
     * @param onResult Callback with success status and message.
     */
    fun updateIncidentStatus(incidentId: String, newStatus: String, onResult: (Boolean, String?) -> Unit) {
        if (incidentId.isBlank() || newStatus.isBlank()) {
            onResult(false, "Incident ID and new status cannot be blank.")
            return
        }
        viewModelScope.launch {
            _isIncidentsLoading.value = true // Can use a more specific loading state if preferred
            val result = incidentRepository.updateIncidentStatus(incidentId, newStatus)
            _isIncidentsLoading.value = false
            if (result.isSuccess) {
                Log.d("AdminViewModel", "Incident $incidentId status updated to $newStatus.")
                onResult(true, "Incident status updated successfully.")
                // The live query from observeIncidents should automatically update the list.
            } else {
                val errorMsg = "Failed to update incident status: ${result.exceptionOrNull()?.message}"
                Log.e("AdminViewModel", errorMsg)
                _incidentError.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }
    // --- End of Incident Management Functions ---

    // --- User Management Functions (existing) ---
    fun fetchUsers(
        searchQuery: String = "",
        campusId: String? = null,
        role: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                var query: Query = db.collection("users")
                    .orderBy("createdAt", Query.Direction.DESCENDING)

                if (!campusId.isNullOrBlank()) {
                    query = query.whereEqualTo("campusId", campusId)
                }
                if (!role.isNullOrBlank()) {
                    query = query.whereEqualTo("role", role)
                }

                query.addSnapshotListener { snapshot, e ->
                    _isLoading.value = false
                    if (e != null) {
                        Log.w("AdminViewModel", "Listen failed for users.", e)
                        _error.value = "Failed to load users: ${e.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val userList = snapshot.documents.mapNotNull { doc ->
                            val user = doc.toObject<User>()
                            user?.copy(id = doc.id)
                        }.filter { user ->
                            searchQuery.isBlank() ||
                                    user.name.contains(searchQuery, ignoreCase = true) ||
                                    user.email.contains(searchQuery, ignoreCase = true)
                        }
                        _users.value = userList
                        _error.value = null
                    } else {
                        _error.value = "No user data found."
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error setting up user fetch listener", e)
                _error.value = "Failed to initiate user loading: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun setSelectedUser(user: User?) {
        _selectedUser.value = user
    }

    fun clearSelectedUser() {
        _selectedUser.value = null
    }

    fun createUser(
        email: String,
        name: String,
        phone: String,
        campusId: String,
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (email.isBlank() || name.isBlank() || phone.isBlank() || campusId.isBlank() || role.isBlank()) {
                onResult(false, "All fields must be filled.")
                _isLoading.value = false
                return@launch
            }
            try {
                val newUser = User(
                    email = email, name = name, phone = phone, campusId = campusId,
                    role = role, createdAt = Timestamp.now(), fcmToken = ""
                )
                db.collection("users").add(newUser).await()
                onResult(true, "User created in Firestore. Auth account needs separate setup.")
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error creating user", e)
                val errorMessage = e.message ?: "Unknown error during user creation."
                _error.value = errorMessage
                onResult(false, "Failed to create user: $errorMessage")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUser(
        userId: String,
        updatedUserData: Map<String, Any>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (userId.isBlank()) {
                onResult(false, "User ID is missing for update.")
                _isLoading.value = false
                return@launch
            }
            try {
                val finalUserData = updatedUserData.toMutableMap()
                finalUserData["updatedAt"] = Timestamp.now()
                db.collection("users").document(userId).update(finalUserData).await()
                onResult(true, "User updated successfully.")
                clearSelectedUser()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error updating user", e)
                val errorMessage = e.message ?: "Unknown error during user update."
                _error.value = errorMessage
                onResult(false, "Failed to update user: $errorMessage")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(userId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (userId.isBlank()) {
                onResult(false, "User ID is missing for deletion.")
                _isLoading.value = false
                return@launch
            }
            try {
                db.collection("users").document(userId).delete().await()
                onResult(true, "User document deleted. Remember to handle Auth deletion.")
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting user", e)
                val errorMessage = e.message ?: "Unknown error during user deletion."
                _error.value = errorMessage
                onResult(false, "Failed to delete user document: $errorMessage")
            } finally {
                _isLoading.value = false
            }
        }
    }
    // --- End of User Management Functions ---

    override fun onCleared() {
        super.onCleared()
        Log.d("AdminViewModel", "AdminViewModel cleared.")
        // Cancel any ongoing coroutines or listeners here if necessary.
        // For example, if IncidentRepository exposed a way to cancel its listener,
        // you would call that here.
    }
}

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getDashboardData(): AdminDashboardAnalytics {
        return try {
            // Implement your actual Firestore query here
            val snapshot = db.collection("admin_analytics")
                .document("dashboard")
                .get()
                .await()

            Log.d("FirestoreDebug", "Raw data: ${snapshot.data}")

            snapshot.toObject(AdminDashboardAnalytics::class.java) ?:
            throw Exception("No analytics data found")
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error fetching analytics", e)
            throw e
        }
    }
}