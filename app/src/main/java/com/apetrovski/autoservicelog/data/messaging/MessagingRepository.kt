package com.apetrovski.autoservicelog.data.messaging

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging

data class WorkNotification(
    val id: String,
    val worksheetId: String,
    val carId: String,
    val mechanicName: String,
    val manufacturer: String,
    val model: String,
    val licensePlate: String
)

class MessagingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {
    fun saveCurrentToken(onResult: (Result<Unit>) -> Unit = {}) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return
        }

        messaging.token
            .addOnSuccessListener { token ->
                saveToken(user.uid, token, onResult)
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun saveTokenForCurrentUser(
        token: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return
        }

        saveToken(user.uid, token, onResult)
    }

    fun observePendingWorkNotifications(
        onResult: (Result<List<WorkNotification>>) -> Unit
    ): ListenerRegistration? {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.success(emptyList()))
            return null
        }

        return firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("ownerId", user.uid)
            .whereEqualTo("delivered", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val notifications = snapshot
                    ?.documents
                    ?.map { document ->
                        WorkNotification(
                            id = document.getString("id") ?: document.id,
                            worksheetId = document.getString("worksheetId").orEmpty(),
                            carId = document.getString("carId").orEmpty(),
                            mechanicName = document.getString("mechanicName").orEmpty(),
                            manufacturer = document.getString("manufacturer").orEmpty(),
                            model = document.getString("model").orEmpty(),
                            licensePlate = document.getString("licensePlate").orEmpty()
                        )
                    }
                    ?: emptyList()

                onResult(Result.success(notifications))
            }
    }

    fun markNotificationDelivered(notificationId: String) {
        if (notificationId.isBlank()) return

        firestore.collection(NOTIFICATIONS_COLLECTION)
            .document(notificationId)
            .update(
                mapOf(
                    "delivered" to true,
                    "deliveredAt" to FieldValue.serverTimestamp()
                )
            )
    }

    private fun saveToken(
        uid: String,
        token: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val data = hashMapOf(
            "token" to token,
            "uid" to uid,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(TOKENS_COLLECTION)
            .document(tokenDocumentId(token))
            .set(data)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun tokenDocumentId(token: String): String {
        return token.replace("/", "_")
    }

    companion object {
        private const val NOTIFICATIONS_COLLECTION = "notifications"
        private const val USERS_COLLECTION = "users"
        private const val TOKENS_COLLECTION = "fcmTokens"
    }
}
