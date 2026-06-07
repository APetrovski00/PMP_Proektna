package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRecord
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorksheetFragment : Fragment(R.layout.screen_worksheet) {
    private val worksheetRepository = WorksheetRepository()
    private var worksheetListener: ListenerRegistration? = null

    private var carText: TextView? = null
    private var plateText: TextView? = null
    private var statusText: TextView? = null
    private var startedText: TextView? = null
    private var workDescriptionInput: EditText? = null
    private var saveButton: View? = null
    private var finishButton: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carText = view.findViewById(R.id.worksheetCarText)
        plateText = view.findViewById(R.id.worksheetPlateText)
        statusText = view.findViewById(R.id.worksheetStatusText)
        startedText = view.findViewById(R.id.worksheetStartedText)
        workDescriptionInput = view.findViewById(R.id.workDescriptionInput)
        saveButton = view.findViewById(R.id.saveNoteButton)
        finishButton = view.findViewById(R.id.finishWorkButton)

        saveButton?.setOnClickListener {
            saveWorkDescription()
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
        saveButton = null
        finishButton = null
        super.onDestroyView()
    }

    private fun saveWorkDescription() {
        val worksheetId = arguments?.getString(ARG_WORKSHEET_ID).orEmpty()
        val description = workDescriptionInput?.text?.toString()?.trim().orEmpty()

        if (worksheetId.isBlank()) {
            showMessage(R.string.worksheet_load_failed)
            return
        }
        if (description.isBlank()) {
            showMessage(R.string.auth_fill_all_fields)
            return
        }

        saveButton?.isEnabled = false
        worksheetRepository.saveWorkDescription(worksheetId, description) { result ->
            if (!isAdded) return@saveWorkDescription

            saveButton?.isEnabled = true
            result
                .onSuccess {
                    workDescriptionInput?.text?.clear()
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

        finishButton?.isEnabled = false
        saveButton?.isEnabled = false
        worksheetRepository.finishWorksheet(worksheetId) { result ->
            if (!isAdded) return@finishWorksheet

            finishButton?.isEnabled = true
            saveButton?.isEnabled = true
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

    companion object {
        private const val ARG_WORKSHEET_ID = "worksheetId"
    }
}
