package com.apetrovski.autoservicelog.data.worksheets

import com.apetrovski.autoservicelog.data.cars.CarListItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class WorksheetRecord(
    val id: String,
    val carId: String,
    val ownerId: String,
    val mechanicId: String,
    val mechanicName: String,
    val manufacturer: String,
    val model: String,
    val licensePlate: String,
    val vin: String,
    val status: String,
    val workDescription: String,
    val photoBase64: String,
    val startedAt: Long,
    val finishedAt: Long?
)

class WorksheetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun startWorksheet(
        car: CarListItem,
        onResult: (Result<String>) -> Unit
    ) {
        val mechanic = auth.currentUser
        if (mechanic == null) {
            onResult(Result.failure(IllegalStateException("User not found")))
            return
        }

        val worksheetDocument = firestore.collection(WORKSHEETS_COLLECTION).document()
        val now = Timestamp.now()
        val mechanicName = mechanic.displayName ?: mechanic.email.orEmpty()
        val worksheet = hashMapOf(
            "id" to worksheetDocument.id,
            "carId" to car.id,
            "ownerId" to car.ownerId,
            "ownerName" to car.ownerName,
            "ownerEmail" to car.ownerEmail,
            "mechanicId" to mechanic.uid,
            "mechanicName" to mechanicName,
            "mechanicEmail" to mechanic.email.orEmpty(),
            "manufacturer" to car.manufacturer,
            "model" to car.model,
            "licensePlate" to car.licensePlate,
            "vin" to car.vin,
            "status" to STATUS_ONGOING,
            "workDescription" to "",
            "photoBase64" to "",
            "startedAt" to now,
            "finishedAt" to null,
            "createdAt" to now,
            "updatedAt" to now
        )

        val carDocument = firestore.collection(CARS_COLLECTION).document(car.id)
        val batch = firestore.batch()
        batch.set(worksheetDocument, worksheet)
        batch.update(
            carDocument,
            mapOf<String, Any>(
                "worksheetCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                onResult(Result.success(worksheetDocument.id))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun observeWorksheet(
        worksheetId: String,
        onResult: (Result<WorksheetRecord?>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(WORKSHEETS_COLLECTION)
            .document(worksheetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onResult(Result.success(null))
                    return@addSnapshotListener
                }

                onResult(Result.success(snapshot.toWorksheetRecord()))
            }
    }

    fun observeWorksheetsForCar(
        carId: String,
        onResult: (Result<List<WorksheetRecord>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(WORKSHEETS_COLLECTION)
            .whereEqualTo("carId", carId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val worksheets = snapshot
                    ?.documents
                    ?.map { document -> document.toWorksheetRecord() }
                    ?.sortedByDescending { worksheet -> worksheet.startedAt }
                    ?: emptyList()

                onResult(Result.success(worksheets))
            }
    }

    fun saveWorkDescription(
        worksheetId: String,
        workDescription: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        saveWorksheetUpdate(worksheetId, workDescription, null, onResult)
    }

    fun saveWorksheetUpdate(
        worksheetId: String,
        workDescription: String,
        photoBase64: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        saveWorksheetFields(worksheetId, workDescription, photoBase64, onResult)
    }

    private fun saveWorksheetFields(
        worksheetId: String,
        workDescription: String,
        photoBase64: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        val worksheetDocument = firestore.collection(WORKSHEETS_COLLECTION).document(worksheetId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(worksheetDocument)
            val currentDescription = snapshot.getString("workDescription").orEmpty()
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (workDescription.isNotBlank()) {
                updates["workDescription"] = if (currentDescription.isBlank()) {
                    workDescription
                } else {
                    "$currentDescription\n\n$workDescription"
                }
            }
            if (photoBase64 != null) {
                updates["photoBase64"] = photoBase64
            }

            transaction.update(worksheetDocument, updates)
        }
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun finishWorksheet(
        worksheetId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection(WORKSHEETS_COLLECTION)
            .document(worksheetId)
            .update(
                mapOf(
                    "status" to STATUS_FINISHED,
                    "finishedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    companion object {
        const val STATUS_ONGOING = "Ongoing"
        const val STATUS_FINISHED = "Finished"

        private const val CARS_COLLECTION = "cars"
        private const val WORKSHEETS_COLLECTION = "worksheets"
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toWorksheetRecord(): WorksheetRecord {
    return WorksheetRecord(
        id = getString("id") ?: id,
        carId = getString("carId").orEmpty(),
        ownerId = getString("ownerId").orEmpty(),
        mechanicId = getString("mechanicId").orEmpty(),
        mechanicName = getString("mechanicName").orEmpty(),
        manufacturer = getString("manufacturer").orEmpty(),
        model = getString("model").orEmpty(),
        licensePlate = getString("licensePlate").orEmpty(),
        vin = getString("vin").orEmpty(),
        status = getString("status").orEmpty(),
        workDescription = getString("workDescription").orEmpty(),
        photoBase64 = getString("photoBase64").orEmpty(),
        startedAt = getTimestamp("startedAt")?.toDate()?.time ?: 0L,
        finishedAt = getTimestamp("finishedAt")?.toDate()?.time
    )
}
