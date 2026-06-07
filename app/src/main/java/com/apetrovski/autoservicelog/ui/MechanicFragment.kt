package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.analytics.AppAnalytics
import com.apetrovski.autoservicelog.data.cars.CarListItem
import com.apetrovski.autoservicelog.data.cars.CarRepository
import com.apetrovski.autoservicelog.data.worksheets.WorksheetRepository

class MechanicFragment : Fragment(R.layout.screen_mechanic) {
    private val carRepository = CarRepository()
    private val worksheetRepository = WorksheetRepository()
    private var selectedCar: CarListItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plateSearchInput = view.findViewById<EditText>(R.id.plateSearchInput)
        val searchButton = view.findViewById<View>(R.id.searchButton)
        val searchResult = view.findViewById<View>(R.id.searchResult)
        val searchHintText = view.findViewById<TextView>(R.id.searchHintText)
        val resultCarText = view.findViewById<TextView>(R.id.resultCarText)
        val resultPlateText = view.findViewById<TextView>(R.id.resultPlateText)
        val resultVinText = view.findViewById<TextView>(R.id.resultVinText)
        val resultOwnerText = view.findViewById<TextView>(R.id.resultOwnerText)
        val startWorkButton = view.findViewById<View>(R.id.startWorkButton)

        startWorkButton.isEnabled = false
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
                            searchHintText.text = getString(R.string.car_not_found)
                        } else {
                            showCarResult(
                                car,
                                searchResult,
                                searchHintText,
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
                        searchHintText.text = getString(R.string.search_car_failed)
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

    private fun showCarResult(
        car: CarListItem,
        searchResult: View,
        searchHintText: TextView,
        resultCarText: TextView,
        resultPlateText: TextView,
        resultVinText: TextView,
        resultOwnerText: TextView
    ) {
        searchResult.visibility = View.VISIBLE
        searchHintText.text = getString(R.string.search_car_found)
        resultCarText.text = "${car.manufacturer} ${car.model}"
        resultPlateText.text = car.licensePlate
        resultVinText.text = car.vin
        resultOwnerText.text = getString(R.string.owner_result, ownerText(car))
    }

    private fun ownerText(car: CarListItem): String {
        return car.ownerName.ifBlank { car.ownerEmail }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_WORKSHEET_ID = "worksheetId"
    }
}
