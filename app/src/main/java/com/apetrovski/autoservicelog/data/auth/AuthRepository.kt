package com.apetrovski.autoservicelog.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class AuthUserProfile(
    val uid: String,
    val email: String,
    val role: String
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun loginWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("User not found")))
                    return@addOnSuccessListener
                }

                loadUserProfile(user.uid, onResult)
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun createAccount(
        email: String,
        password: String,
        role: String,
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("User not found")))
                    return@addOnSuccessListener
                }

                val profile = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(profile)
                    .addOnSuccessListener {
                        onResult(Result.success(AuthUserProfile(user.uid, email, role)))
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun loginWithGoogle(
        idToken: String,
        onResult: (Result<AuthUserProfile?>) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("User not found")))
                    return@addOnSuccessListener
                }

                loadUserProfileIfExists(user.uid, onResult)
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun saveCurrentUserRole(
        role: String,
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return
        }

        saveUserProfile(user.uid, user.email.orEmpty(), role, onResult)
    }

    fun logout() {
        auth.signOut()
    }

    private fun loadUserProfile(
        uid: String,
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val email = document.getString("email")
                val role = document.getString("role")

                if (email.isNullOrBlank() || role.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("User profile is incomplete")))
                    return@addOnSuccessListener
                }

                onResult(Result.success(AuthUserProfile(uid, email, role)))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun loadUserProfileIfExists(
        uid: String,
        onResult: (Result<AuthUserProfile?>) -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onResult(Result.success(null))
                    return@addOnSuccessListener
                }

                val email = document.getString("email")
                val role = document.getString("role")

                if (email.isNullOrBlank() || role.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("User profile is incomplete")))
                    return@addOnSuccessListener
                }

                onResult(Result.success(AuthUserProfile(uid, email, role)))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun saveUserProfile(
        uid: String,
        email: String,
        role: String,
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        val profile = hashMapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(profile)
            .addOnSuccessListener {
                onResult(Result.success(AuthUserProfile(uid, email, role)))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    companion object {
        const val ROLE_OWNER = "owner"
        const val ROLE_MECHANIC = "mechanic"

        private const val USERS_COLLECTION = "users"
    }
}
