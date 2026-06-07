package com.apetrovski.autoservicelog.ui

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRecord
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRepository
import com.google.firebase.firestore.ListenerRegistration
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class WorksheetFragment : Fragment(R.layout.screen_worksheet) {
    private val worksheetRepository = WorksheetRepository()
    private var worksheetListener: ListenerRegistration? = null
    private var selectedPhotoUri: Uri? = null
    private var cameraPhotoUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val photoUri = cameraPhotoUri
        if (success && photoUri != null) {
            setSelectedPhoto(photoUri)
        } else {
            cameraPhotoUri = null
        }
    }

    private val addPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { photoUri ->
        if (photoUri != null) {
            setSelectedPhoto(photoUri)
        }
    }

    private var carText: TextView? = null
    private var plateText: TextView? = null
    private var statusText: TextView? = null
    private var startedText: TextView? = null
    private var workDescriptionInput: EditText? = null
    private var addPhotoButton: View? = null
    private var photoPreviewImage: ImageView? = null
    private var deletePhotoButton: View? = null
    private var saveButton: View? = null
    private var finishButton: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carText = view.findViewById(R.id.worksheetCarText)
        plateText = view.findViewById(R.id.worksheetPlateText)
        statusText = view.findViewById(R.id.worksheetStatusText)
        startedText = view.findViewById(R.id.worksheetStartedText)
        workDescriptionInput = view.findViewById(R.id.workDescriptionInput)
        addPhotoButton = view.findViewById(R.id.addPhotoButton)
        photoPreviewImage = view.findViewById(R.id.photoPreviewImage)
        deletePhotoButton = view.findViewById(R.id.deletePhotoButton)
        saveButton = view.findViewById(R.id.saveNoteButton)
        finishButton = view.findViewById(R.id.finishWorkButton)

        addPhotoButton?.setOnClickListener {
            showPhotoOptions()
        }
        deletePhotoButton?.setOnClickListener {
            clearSelectedPhoto()
        }
        saveButton?.setOnClickListener {
            saveWorksheet()
        }
        finishButton?.setOnClickListener {
            finishWorksheet()
        }
    }

    override fun onStart() {
        super.onStart()

        val worksheetId = arguments?.getString(ARG_WORKSHEET_ID).orEmpty()
        if (worksheetId.isBlank()) {
            showMessage(R.string.worksheet_load_failed)
            return
        }

        worksheetListener?.remove()
        worksheetListener = worksheetRepository.observeWorksheet(worksheetId) { result ->
            if (!isAdded) return@observeWorksheet

            result
                .onSuccess { worksheet ->
                    if (worksheet == null) {
                        showMessage(R.string.worksheet_load_failed)
                    } else {
                        showWorksheet(worksheet)
                    }
                }
                .onFailure {
                    showMessage(R.string.worksheet_load_failed)
                }
        }
    }

    override fun onStop() {
        worksheetListener?.remove()
        worksheetListener = null
        super.onStop()
    }

    override fun onDestroyView() {
        carText = null
        plateText = null
        statusText = null
        startedText = null
        workDescriptionInput = null
        addPhotoButton = null
        photoPreviewImage = null
        deletePhotoButton = null
        saveButton = null
        finishButton = null
        super.onDestroyView()
    }

    private fun showPhotoOptions() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val takePhotoButton = createDialogButton(getString(R.string.take_photo))
        val addPhotoButton = createDialogButton(getString(R.string.add_photo))
        content.addView(takePhotoButton)
        content.addView(addPhotoButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(content)
            .create()

        takePhotoButton.setOnClickListener {
            dialog.dismiss()
            openCamera()
        }
        addPhotoButton.setOnClickListener {
            dialog.dismiss()
            addPhotoLauncher.launch("image/*")
        }

        dialog.show()
    }

    private fun createDialogButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            setAllCaps(false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoUri = createCameraPhotoUri()
            cameraPhotoUri = photoUri
            takePhotoLauncher.launch(photoUri)
        } catch (_: Exception) {
            showMessage(R.string.photo_select_failed)
        }
    }

    private fun createCameraPhotoUri(): Uri {
        val photoFile = File.createTempFile("worksheet_photo_", ".jpg", requireContext().cacheDir)
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
    }

    private fun setSelectedPhoto(photoUri: Uri) {
        selectedPhotoUri = photoUri
        photoPreviewImage?.setImageURI(photoUri)
        photoPreviewImage?.visibility = View.VISIBLE
        deletePhotoButton?.visibility = View.VISIBLE
    }

    private fun clearSelectedPhoto() {
        selectedPhotoUri = null
        cameraPhotoUri = null
        photoPreviewImage?.setImageDrawable(null)
        photoPreviewImage?.visibility = View.GONE
        deletePhotoButton?.visibility = View.GONE
    }

    private fun encodePhoto(photoUri: Uri): String? {
        val bitmap = loadBitmap(photoUri) ?: return null
        val resizedBitmap = resizeBitmap(bitmap, MAX_PHOTO_SIZE)
        val output = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun loadBitmap(photoUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                requireContext().contentResolver.openInputStream(photoUri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxSize) return bitmap

        val ratio = maxSize.toFloat() / largestSide.toFloat()
        val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun saveWorksheet() {
        val worksheetId = arguments?.getString(ARG_WORKSHEET_ID).orEmpty()
        val description = workDescriptionInput?.text?.toString()?.trim().orEmpty()
        val photoUri = selectedPhotoUri

        if (worksheetId.isBlank()) {
            showMessage(R.string.worksheet_load_failed)
            return
        }
        if (description.isBlank() && photoUri == null) {
            showMessage(R.string.auth_fill_all_fields)
            return
        }

        val photoBase64 = if (photoUri == null) {
            null
        } else {
            encodePhoto(photoUri)
        }
        if (photoUri != null && photoBase64 == null) {
            showMessage(R.string.photo_select_failed)
            return
        }

        setControlsEnabled(false)
        worksheetRepository.saveWorksheetUpdate(worksheetId, description, photoBase64) { result ->
            if (!isAdded) return@saveWorksheetUpdate

            setControlsEnabled(true)
            result
                .onSuccess {
                    workDescriptionInput?.text?.clear()
                    clearSelectedPhoto()
                    showMessage(R.string.work_saved)
                }
                .onFailure {
                    showMessage(R.string.work_save_failed)
                }
        }
    }

    private fun finishWorksheet() {
        val worksheetId = arguments?.getString(ARG_WORKSHEET_ID).orEmpty()
        if (worksheetId.isBlank()) {
            showMessage(R.string.worksheet_load_failed)
            return
        }

        setControlsEnabled(false)
        worksheetRepository.finishWorksheet(worksheetId) { result ->
            if (!isAdded) return@finishWorksheet

            setControlsEnabled(true)
            result
                .onSuccess {
                    showMessage(R.string.work_finished)
                    findNavController().popBackStack()
                }
                .onFailure {
                    showMessage(R.string.finish_work_failed)
                }
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        addPhotoButton?.isEnabled = enabled
        deletePhotoButton?.isEnabled = enabled
        saveButton?.isEnabled = enabled
        finishButton?.isEnabled = enabled
    }

    private fun showWorksheet(worksheet: WorksheetRecord) {
        carText?.text = "${worksheet.manufacturer} ${worksheet.model}"
        plateText?.text = worksheet.licensePlate
        statusText?.text = getString(R.string.status_value, worksheet.status)
        startedText?.text = getString(R.string.started_value, formatDate(worksheet.startedAt))
    }

    private fun formatDate(time: Long): String {
        if (time == 0L) return ""
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(time))
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ARG_WORKSHEET_ID = "worksheetId"
        private const val MAX_PHOTO_SIZE = 700
        private const val PHOTO_QUALITY = 65
    }
}
