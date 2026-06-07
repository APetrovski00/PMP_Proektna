package com.apetrovski.autoservicelog.data.cars

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

data class CarForm(
    val licensePlate: String,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val vin: String,
    val color: String
)

data class CarListItem(
    val id: String,
    val manufacturer: String,
    val model: String,
    val licensePlate: String,
    val vin: String,
    val worksheetCount: Int,
    val createdAt: Long
)

class CarRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun addCar(
        car: CarForm,
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return
        }

        val carDocument = firestore.collection(CARS_COLLECTION).document()
        val ownerName = user.displayName ?: user.email.orEmpty()
        val data = hashMapOf(
            "id" to carDocument.id,
            "ownerId" to user.uid,
            "ownerName" to ownerName,
            "ownerEmail" to user.email.orEmpty(),
            "licensePlate" to car.licensePlate,
            "licensePlateSearch" to normalizeLicensePlate(car.licensePlate),
            "manufacturer" to car.manufacturer,
            "model" to car.model,
            "year" to car.year,
            "vin" to car.vin,
            "color" to car.color,
            "worksheetCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        carDocument
            .set(data)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun observeCurrentOwnerCars(
        onResult: (Result<List<CarListItem>>) -> Unit
    ): ListenerRegistration? {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return null
        }

        return firestore.collection(CARS_COLLECTION)
            .whereEqualTo("ownerId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val cars = snapshot
                    ?.documents
                    ?.map { document ->
                        CarListItem(
                            id = document.getString("id") ?: document.id,
                            manufacturer = document.getString("manufacturer").orEmpty(),
                            model = document.getString("model").orEmpty(),
                            licensePlate = document.getString("licensePlate").orEmpty(),
                            vin = document.getString("vin").orEmpty(),
                            worksheetCount = document.getLong("worksheetCount")?.toInt() ?: 0,
                            createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        )
                    }
                    ?.sortedByDescending { car -> car.createdAt }
                    ?: emptyList()

                onResult(Result.success(cars))
            }
    }

    private fun normalizeLicensePlate(licensePlate: String): String {
        return licensePlate
            .filter { it.isLetterOrDigit() }
            .uppercase(Locale.ROOT)
    }

    companion object {
        private const val CARS_COLLECTION = "cars"
    }
}
