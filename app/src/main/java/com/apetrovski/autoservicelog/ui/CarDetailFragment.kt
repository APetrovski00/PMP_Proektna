package com.apetrovski.autoservicelog.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.analytics.AppAnalytics
import com.apetrovski.autoservicelog.data.cars.CarDetail
import com.apetrovski.autoservicelog.data.cars.CarRepository
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRecord
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CarDetailFragment : Fragment(R.layout.screen_car_detail) {
    private val carRepository = CarRepository()
    private val worksheetRepository = WorksheetRepository()
    private var carListener: ListenerRegistration? = null
    private var worksheetListener: ListenerRegistration? = null

    private var carNameText: TextView? = null
    private var carPlateText: TextView? = null
    private var carVinText: TextView? = null
    private var carYearText: TextView? = null
    private var carColorText: TextView? = null
    private var worksheetList: LinearLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carNameText = view.findViewById(R.id.carNameText)
        carPlateText = view.findViewById(R.id.carPlateText)
        carVinText = view.findViewById(R.id.carVinText)
        carYearText = view.findViewById(R.id.carYearText)
        carColorText = view.findViewById(R.id.carColorText)
        worksheetList = view.findViewById(R.id.worksheetList)
    }

    override fun onStart() {
        super.onStart()

        val carId = arguments?.getString(ARG_CAR_ID).orEmpty()
        if (carId.isBlank()) {
            showMessage(R.string.car_load_failed)
            return
        }

        startCarListener(carId)
        startWorksheetListener(carId)
    }

    override fun onStop() {
        carListener?.remove()
        carListener = null
        worksheetListener?.remove()
        worksheetListener = null
        super.onStop()
    }

    override fun onDestroyView() {
        carNameText = null
        carPlateText = null
        carVinText = null
        carYearText = null
        carColorText = null
        worksheetList = null
        super.onDestroyView()
    }

    private fun startCarListener(carId: String) {
        carListener?.remove()
        carListener = carRepository.observeCar(carId) { result ->
            if (!isAdded) return@observeCar

            result
                .onSuccess { car ->
                    if (car == null) {
                        showMessage(R.string.car_load_failed)
                    } else {
                        showCar(car)
                    }
                }
                .onFailure {
                    showMessage(R.string.car_load_failed)
                }
        }
    }

    private fun startWorksheetListener(carId: String) {
        worksheetListener?.remove()
        worksheetListener = worksheetRepository.observeWorksheetsForCar(carId) { result ->
            if (!isAdded) return@observeWorksheetsForCar

            result
                .onSuccess { worksheets ->
                    showWorksheets(worksheets)
                }
                .onFailure {
                    showMessage(R.string.worksheet_load_failed)
                }
        }
    }

    private fun showCar(car: CarDetail) {
        carNameText?.text = "${car.manufacturer} ${car.model}"
        carPlateText?.text = getString(R.string.license_plate_value, car.licensePlate)
        carVinText?.text = getString(R.string.vin_value, car.vin)
        carYearText?.text = getString(R.string.year_value, car.year)
        carColorText?.text = getString(R.string.color_value, car.color)
    }

    private fun showWorksheets(worksheets: List<WorksheetRecord>) {
        val list = worksheetList ?: return
        list.removeAllViews()

        if (worksheets.isEmpty()) {
            list.addView(createEmptyView())
            return
        }

        worksheets.forEachIndexed { index, worksheet ->
            list.addView(createWorksheetRow(worksheet))
            if (index < worksheets.lastIndex) {
                list.addView(createDivider())
            }
        }
    }

    private fun createWorksheetRow(worksheet: WorksheetRecord): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            foreground = attributes.getDrawable(0)
            attributes.recycle()
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener {
                AppAnalytics.worksheetOpened(requireContext())
                findNavController().navigate(
                    R.id.action_carDetailFragment_to_worksheetDetailFragment,
                    Bundle().apply {
                        putString(ARG_WORKSHEET_ID, worksheet.id)
                    }
                )
            }

            addView(createTextView(getString(R.string.status_value, worksheet.status), 15f, true))
            addView(createTextView(mechanicText(worksheet), 14f, false, topMarginDp = 6))
            addView(createTextView(getString(R.string.started_value, formatDate(worksheet.startedAt)), 14f, false, topMarginDp = 6))
            worksheet.finishedAt?.let { finishedAt ->
                addView(createTextView(getString(R.string.finished_value, formatDate(finishedAt)), 14f, false, topMarginDp = 6))
            }
            addView(createTextView(workDescriptionText(worksheet), 14f, false, topMarginDp = 8))
        }
    }

    private fun createTextView(
        textValue: String,
        textSizeSp: Float,
        bold: Boolean,
        topMarginDp: Int = 0
    ): TextView {
        return TextView(requireContext()).apply {
            text = textValue
            textSize = textSizeSp
            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(topMarginDp)
            }
        }
    }

    private fun createDivider(): View {
        return View(requireContext()).apply {
            setBackgroundResource(android.R.color.darker_gray)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }
    }

    private fun createEmptyView(): View {
        return TextView(requireContext()).apply {
            text = getString(R.string.no_worksheets_yet)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
        }
    }

    private fun mechanicText(worksheet: WorksheetRecord): String {
        return getString(R.string.mechanic_value, worksheet.mechanicName.ifBlank { "-" })
    }

    private fun workDescriptionText(worksheet: WorksheetRecord): String {
        return worksheet.workDescription.ifBlank {
            getString(R.string.no_work_description)
        }
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
        private const val ARG_CAR_ID = "carId"
        private const val ARG_WORKSHEET_ID = "worksheetId"
    }
}
