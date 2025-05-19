package com.example.myapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val user: StateFlow<User?> = _user.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String,
        campusId: String,
        role: String
    ) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("User creation failed")

                val newUser = User(
                    email = email,
                    name = name,
                    phone = phone,
                    campusId = campusId,
                    role = role,
                    createdAt = Timestamp.now()
                )

                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                _user.value = newUser
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("Sign-in failed")

                val userDoc = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                _user.value = userDoc.toObject(User::class.java)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        _error.value = null
    }

    fun sendVerificationEmail(onComplete: (Boolean) -> Unit = { _ -> }) {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser ?: throw Exception("No user logged in")
                firebaseUser.sendEmailVerification().await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun reportTestIncident(user: User?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val incident = Incident(
                    reportedBy = user?.email ?: "",
                    campusId = user?.campusId ?: "",
                    type = "test",
                    description = "Test incident",
                    createdAt = Timestamp.now()
                )
                FirebaseFirestore.getInstance()
                    .collection("incidents")
                    .add(incident)
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun checkAuthState() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            viewModelScope.launch {
                try {
                    val userDoc = firestore.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()

                    _user.value = userDoc.toObject(User::class.java)
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to check auth state"
                }
            }
        }
    }
}


