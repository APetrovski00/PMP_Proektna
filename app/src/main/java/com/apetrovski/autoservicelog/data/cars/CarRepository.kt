package com.apetrovski.autoservicelog.data.cars

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

data class CarForm(
    val licensePlate: String,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val vin: String,
    val color: String
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

    private fun normalizeLicensePlate(licensePlate: String): String {
        return licensePlate
            .filter { it.isLetterOrDigit() }
            .uppercase(Locale.ROOT)
    }

    companion object {
        private const val CARS_COLLECTION = "cars"
    }
}
