package com.apetrovski.autoservicelog.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class AuthUserProfile(
    val uid: String,
    val email: String,
    val role: String,
    val displayName: String = ""
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
        firstName: String,
        lastName: String,
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
                    "displayName" to buildDisplayName(firstName, lastName),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(profile)
                    .addOnSuccessListener {
                        onResult(Result.success(AuthUserProfile(user.uid, email, role, buildDisplayName(firstName, lastName))))
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

                loadUserProfileIfExists(
                    uid = user.uid,
                    firebaseEmail = user.email.orEmpty(),
                    firebaseDisplayName = user.displayName,
                    onResult = onResult
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun loginWithFacebook(
        accessToken: String,
        onResult: (Result<AuthUserProfile?>) -> Unit
    ) {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("User not found")))
                    return@addOnSuccessListener
                }

                loadUserProfileIfExists(
                    uid = user.uid,
                    firebaseEmail = user.email.orEmpty(),
                    firebaseDisplayName = user.displayName,
                    onResult = onResult
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun loginAnonymously(
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("User not found")))
                    return@addOnSuccessListener
                }

                val profile = hashMapOf(
                    "uid" to user.uid,
                    "email" to "",
                    "role" to ROLE_ANONYMOUS,
                    "displayName" to ANONYMOUS_DISPLAY_NAME,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(profile)
                    .addOnSuccessListener {
                        onResult(
                            Result.success(
                                AuthUserProfile(
                                    uid = user.uid,
                                    email = "",
                                    role = ROLE_ANONYMOUS,
                                    displayName = ANONYMOUS_DISPLAY_NAME
                                )
                            )
                        )
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
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

    fun loadCurrentUserProfile(
        onResult: (Result<AuthUserProfile>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return
        }

        if (user.isAnonymous) {
            onResult(
                Result.success(
                    AuthUserProfile(
                        uid = user.uid,
                        email = "",
                        role = ROLE_ANONYMOUS,
                        displayName = ANONYMOUS_DISPLAY_NAME
                    )
                )
            )
            return
        }

        loadUserProfile(user.uid, onResult)
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
                val email = document.getString("email").orEmpty()
                val role = document.getString("role")

                if (role.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("User profile is incomplete")))
                    return@addOnSuccessListener
                }

                onResult(Result.success(AuthUserProfile(uid, email, role, formatProfileName(document.getString("displayName"), email))))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun loadUserProfileIfExists(
        uid: String,
        firebaseEmail: String,
        firebaseDisplayName: String?,
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

                val email = document.getString("email").orEmpty().ifBlank { firebaseEmail }
                val role = document.getString("role")

                if (role.isNullOrBlank()) {
                    onResult(Result.success(null))
                    return@addOnSuccessListener
                }

                onResult(
                    Result.success(
                        AuthUserProfile(
                            uid = uid,
                            email = email,
                            role = role,
                            displayName = formatProfileName(
                                document.getString("displayName") ?: firebaseDisplayName,
                                email
                            )
                        )
                    )
                )
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
            "displayName" to auth.currentUser?.displayName.orEmpty(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(profile)
            .addOnSuccessListener {
                onResult(Result.success(AuthUserProfile(uid, email, role, formatProfileName(auth.currentUser?.displayName, email))))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun buildDisplayName(firstName: String, lastName: String): String {
        return listOf(firstName.trim(), lastName.trim())
            .filter { value -> value.isNotBlank() }
            .map { value -> formatNamePart(value) }
            .joinToString(" ")
    }

    private fun formatNamePart(value: String): String {
        val trimmed = value.trim()
        return trimmed.take(1).uppercase() + trimmed.drop(1).lowercase()
    }

    companion object {
        const val ROLE_OWNER = "owner"
        const val ROLE_MECHANIC = "mechanic"
        const val ROLE_ANONYMOUS = "anonymous"

        private const val ANONYMOUS_DISPLAY_NAME = "Anonymous"
        private const val USERS_COLLECTION = "users"

        fun formatProfileName(displayName: String?, email: String): String {
            displayName?.trim()?.takeIf { value -> value.isNotBlank() }?.let { value ->
                if (value.contains("@")) {
                    return formatProfileName(null, value)
                }
                if (!value.contains(" ") && value.contains(".")) {
                    val domain = email.substringAfter("@", "")
                    return formatProfileName(null, "$value@$domain")
                }
                return value
            }

            val localPart = email.substringBefore("@").trim()
            if (localPart.isBlank()) return ANONYMOUS_DISPLAY_NAME

            val nameParts = localPart
                .split('.', '_', '-')
                .filter { part -> part.isNotBlank() }

            if (nameParts.size >= 2) {
                val domain = email.substringAfter("@", "")
                val orderedParts = if (domain.contains("uklo", ignoreCase = true)) {
                    nameParts.reversed()
                } else {
                    nameParts
                }

                return orderedParts.joinToString(" ") { part ->
                    part.take(1).uppercase() + part.drop(1).lowercase()
                }
            }

            return localPart
        }
    }
}
