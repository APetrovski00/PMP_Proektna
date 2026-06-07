package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRecord
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorksheetDetailFragment : Fragment(R.layout.screen_worksheet_detail) {
    private val worksheetRepository = WorksheetRepository()
    private var worksheetListener: ListenerRegistration? = null

    private var detailCarText: TextView? = null
    private var detailPlateText: TextView? = null
    private var detailStatusText: TextView? = null
    private var detailStartedText: TextView? = null
    private var detailFinishedText: TextView? = null
    private var detailMechanicText: TextView? = null
    private var detailWorkDoneText: TextView? = null
    private var photoPlaceholderText: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailCarText = view.findViewById(R.id.detailCarText)
        detailPlateText = view.findViewById(R.id.detailPlateText)
        detailStatusText = view.findViewById(R.id.detailStatusText)
        detailStartedText = view.findViewById(R.id.detailStartedText)
        detailFinishedText = view.findViewById(R.id.detailFinishedText)
        detailMechanicText = view.findViewById(R.id.detailMechanicText)
        detailWorkDoneText = view.findViewById(R.id.detailWorkDoneText)
        photoPlaceholderText = view.findViewById(R.id.photoPlaceholderText)
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
        detailCarText = null
        detailPlateText = null
        detailStatusText = null
        detailStartedText = null
        detailFinishedText = null
        detailMechanicText = null
        detailWorkDoneText = null
        photoPlaceholderText = null
        super.onDestroyView()
    }

    private fun showWorksheet(worksheet: WorksheetRecord) {
        detailCarText?.text = "${worksheet.manufacturer} ${worksheet.model}"
        detailPlateText?.text = getString(R.string.license_plate_value, worksheet.licensePlate)
        detailStatusText?.text = getString(R.string.status_value, worksheet.status)
        detailStartedText?.text = getString(R.string.started_value, formatDate(worksheet.startedAt))
        detailMechanicText?.text = getString(
            R.string.mechanic_value,
            worksheet.mechanicName.ifBlank { "-" }
        )
        detailWorkDoneText?.text = worksheet.workDescription.ifBlank {
            getString(R.string.no_work_description)
        }
        photoPlaceholderText?.text = if (worksheet.photoUrl.isBlank()) {
            getString(R.string.no_photo)
        } else {
            getString(R.string.photo_attached)
        }

        val finishedAt = worksheet.finishedAt
        if (finishedAt == null) {
            detailFinishedText?.visibility = View.GONE
        } else {
            detailFinishedText?.visibility = View.VISIBLE
            detailFinishedText?.text = getString(R.string.finished_value, formatDate(finishedAt))
        }
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
