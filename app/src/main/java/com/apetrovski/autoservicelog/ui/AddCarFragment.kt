package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.analytics.AppAnalytics
import com.apetrovski.autoservicelog.data.cars.CarForm
import com.apetrovski.autoservicelog.data.cars.CarRepository

class AddCarFragment : Fragment(R.layout.screen_add_car) {
    private val carRepository = CarRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val licensePlateInput = view.findViewById<EditText>(R.id.licensePlateInput)
        val manufacturerInput = view.findViewById<EditText>(R.id.makeInput)
        val modelInput = view.findViewById<EditText>(R.id.modelInput)
        val yearInput = view.findViewById<EditText>(R.id.yearInput)
        val vinInput = view.findViewById<EditText>(R.id.vinInput)
        val colorInput = view.findViewById<EditText>(R.id.colorInput)
        val saveCarButton = view.findViewById<View>(R.id.saveCarButton)
        val cancelAddCarButton = view.findViewById<View>(R.id.cancelAddCarButton)

        cancelAddCarButton.setOnClickListener {
            findNavController().navigateUp()
        }
        saveCarButton.setOnClickListener {
            val carForm = readCarForm(
                licensePlateInput,
                manufacturerInput,
                modelInput,
                yearInput,
                vinInput,
                colorInput
            )

            if (carForm == null) return@setOnClickListener

            setButtonsEnabled(saveCarButton, cancelAddCarButton, false)
            carRepository.addCar(carForm) { result ->
                if (!isAdded) return@addCar

                setButtonsEnabled(saveCarButton, cancelAddCarButton, true)
                result
                    .onSuccess {
                        AppAnalytics.carAdded(requireContext())
                        showMessage(R.string.car_saved)
                        closeAfterSave()
                    }
                    .onFailure {
                        showMessage(R.string.save_car_failed)
                    }
            }
        }
    }

    private fun readCarForm(
        licensePlateInput: EditText,
        manufacturerInput: EditText,
        modelInput: EditText,
        yearInput: EditText,
        vinInput: EditText,
        colorInput: EditText
    ): CarForm? {
        val licensePlate = licensePlateInput.text.toString().trim()
        val manufacturer = manufacturerInput.text.toString().trim()
        val model = modelInput.text.toString().trim()
        val yearText = yearInput.text.toString().trim()
        val vin = vinInput.text.toString().trim()
        val color = colorInput.text.toString().trim()

        if (
            licensePlate.isBlank() ||
            manufacturer.isBlank() ||
            model.isBlank() ||
            yearText.isBlank() ||
            vin.isBlank() ||
            color.isBlank()
        ) {
            showMessage(R.string.auth_fill_all_fields)
            return null
        }

        val year = yearText.toIntOrNull()
        if (year == null) {
            showMessage(R.string.invalid_year)
            return null
        }

        return CarForm(
            licensePlate = licensePlate,
            manufacturer = manufacturer,
            model = model,
            year = year,
            vin = vin,
            color = color
        )
    }

    private fun closeAfterSave() {
        val navController = findNavController()
        val previousDestinationId = navController.previousBackStackEntry?.destination?.id
        if (previousDestinationId == R.id.welcomeFragment) {
            navController.navigate(R.id.action_addCarFragment_to_ownerFragment)
        } else {
            navController.navigateUp()
        }
    }

    private fun setButtonsEnabled(
        saveCarButton: View,
        cancelAddCarButton: View,
        enabled: Boolean
    ) {
        saveCarButton.isEnabled = enabled
        cancelAddCarButton.isEnabled = enabled
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }
}
