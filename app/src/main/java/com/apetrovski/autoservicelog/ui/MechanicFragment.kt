package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.analytics.AppAnalytics
import com.apetrovski.autoservicelog.data.cars.CarListItem
import com.apetrovski.autoservicelog.data.cars.CarRepository
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRecord
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MechanicFragment : Fragment(R.layout.screen_mechanic) {
    private val carRepository = CarRepository()
    private val worksheetRepository = WorksheetRepository()
    private var selectedCar: CarListItem? = null
    private var jobsListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plateSearchInput = view.findViewById<EditText>(R.id.plateSearchInput)
        val searchButton = view.findViewById<View>(R.id.searchButton)
        val searchResult = view.findViewById<View>(R.id.searchResult)
        val resultCarText = view.findViewById<TextView>(R.id.resultCarText)
        val resultPlateText = view.findViewById<TextView>(R.id.resultPlateText)
        val resultVinText = view.findViewById<TextView>(R.id.resultVinText)
        val resultOwnerText = view.findViewById<TextView>(R.id.resultOwnerText)
        val startWorkButton = view.findViewById<View>(R.id.startWorkButton)
        val myJobsList = view.findViewById<LinearLayout>(R.id.myJobsList)
        val myJobsEmptyText = view.findViewById<TextView>(R.id.myJobsEmptyText)

        startWorkButton.isEnabled = false
        observeMyJobs(myJobsList, myJobsEmptyText)

        searchButton.setOnClickListener {
            val licensePlate = plateSearchInput.text.toString().trim()
            if (licensePlate.isBlank()) {
                showMessage(R.string.enter_license_plate)
                return@setOnClickListener
            }

            selectedCar = null
            searchButton.isEnabled = false
            startWorkButton.isEnabled = false
            carRepository.findCarByLicensePlate(licensePlate) { result ->
                if (!isAdded) return@findCarByLicensePlate

                searchButton.isEnabled = true
                result
                    .onSuccess { car ->
                        AppAnalytics.carSearch(requireContext(), car != null)
                        selectedCar = car
                        startWorkButton.isEnabled = car != null
                        if (car == null) {
                            searchResult.visibility = View.GONE
                            showMessage(R.string.car_not_found)
                        } else {
                            showCarResult(
                                car,
                                searchResult,
                                resultCarText,
                                resultPlateText,
                                resultVinText,
                                resultOwnerText
                            )
                        }
                    }
                    .onFailure {
                        selectedCar = null
                        startWorkButton.isEnabled = false
                        searchResult.visibility = View.GONE
                        showMessage(R.string.search_car_failed)
                    }
            }
        }

        startWorkButton.setOnClickListener {
            val car = selectedCar
            if (car == null) {
                showMessage(R.string.search_for_car_hint)
                return@setOnClickListener
            }

            searchButton.isEnabled = false
            startWorkButton.isEnabled = false
            worksheetRepository.startWorksheet(car) { result ->
                if (!isAdded) return@startWorksheet

                searchButton.isEnabled = true
                startWorkButton.isEnabled = selectedCar != null
                result
                    .onSuccess { worksheetId ->
                        AppAnalytics.workStarted(requireContext())
                        showMessage(R.string.work_started)
                        findNavController().navigate(
                            R.id.action_mechanicFragment_to_worksheetFragment,
                            Bundle().apply {
                                putString(ARG_WORKSHEET_ID, worksheetId)
                            }
                        )
                    }
                    .onFailure {
                        showMessage(R.string.start_work_failed)
                    }
            }
        }
    }

    override fun onDestroyView() {
        jobsListener?.remove()
        jobsListener = null
        super.onDestroyView()
    }

    private fun showCarResult(
        car: CarListItem,
        searchResult: View,
        resultCarText: TextView,
        resultPlateText: TextView,
        resultVinText: TextView,
        resultOwnerText: TextView
    ) {
        searchResult.visibility = View.VISIBLE
        resultCarText.text = "${car.manufacturer} ${car.model}"
        resultPlateText.text = car.licensePlate
        resultVinText.text = car.vin
        resultOwnerText.text = getString(R.string.owner_result, ownerText(car))
    }

    private fun observeMyJobs(
        myJobsList: LinearLayout,
        myJobsEmptyText: TextView
    ) {
        jobsListener?.remove()
        jobsListener = worksheetRepository.observeWorksheetsForCurrentMechanic { result ->
            if (!isAdded) return@observeWorksheetsForCurrentMechanic

            result
                .onSuccess { worksheets ->
                    showMyJobs(worksheets, myJobsList, myJobsEmptyText)
                }
                .onFailure {
                    myJobsList.removeAllViews()
                    myJobsEmptyText.visibility = View.VISIBLE
                    myJobsEmptyText.setText(R.string.worksheet_load_failed)
                }
        }
    }

    private fun showMyJobs(
        worksheets: List<WorksheetRecord>,
        myJobsList: LinearLayout,
        myJobsEmptyText: TextView
    ) {
        myJobsList.removeAllViews()
        myJobsEmptyText.visibility = if (worksheets.isEmpty()) View.VISIBLE else View.GONE

        worksheets.forEachIndexed { index, worksheet ->
            myJobsList.addView(createJobRow(worksheet))
            if (index < worksheets.lastIndex) {
                myJobsList.addView(createDivider())
            }
        }
    }

    private fun createJobRow(worksheet: WorksheetRecord): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                openWorksheet(worksheet)
            }
            addView(createTextView("${worksheet.manufacturer} ${worksheet.model}", 17f, true))
            addView(createTextView(worksheet.licensePlate, 14f, false, topMarginDp = 4))
            addView(createTextView(getString(R.string.status_value, worksheet.status), 14f, false, topMarginDp = 4))
            addView(createTextView(getString(R.string.started_value, formatDate(worksheet.startedAt)), 14f, false, topMarginDp = 4))
            if (worksheet.workDescription.isNotBlank()) {
                addView(createTextView(worksheet.workDescription, 14f, false, topMarginDp = 6))
            }
        }
    }

    private fun openWorksheet(worksheet: WorksheetRecord) {
        val actionId = if (worksheet.status == WorksheetRepository.STATUS_ONGOING) {
            R.id.action_mechanicFragment_to_worksheetFragment
        } else {
            R.id.action_mechanicFragment_to_worksheetDetailFragment
        }

        findNavController().navigate(
            actionId,
            Bundle().apply {
                putString(ARG_WORKSHEET_ID, worksheet.id)
            }
        )
    }

    private fun createTextView(
        value: String,
        sizeSp: Float,
        bold: Boolean,
        topMarginDp: Int = 0
    ): TextView {
        return TextView(requireContext()).apply {
            text = value
            textSize = sizeSp
            if (bold) {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
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

    private fun ownerText(car: CarListItem): String {
        return car.ownerName.ifBlank { car.ownerEmail }
    }

    private fun formatDate(timeMillis: Long): String {
        if (timeMillis == 0L) return "-"
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timeMillis))
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ARG_WORKSHEET_ID = "worksheetId"
    }
}
