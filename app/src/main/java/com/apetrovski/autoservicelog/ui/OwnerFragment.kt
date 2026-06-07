package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.analytics.AppAnalytics
import com.apetrovski.autoservicelog.data.cars.CarListItem
import com.apetrovski.autoservicelog.data.cars.CarRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerFragment : Fragment(R.layout.screen_owner) {
    private val carRepository = CarRepository()
    private var carsListener: ListenerRegistration? = null
    private var carList: LinearLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carList = view.findViewById(R.id.carList)

        view.findViewById<View>(R.id.addCarButton).setOnClickListener {
            findNavController().navigate(R.id.action_ownerFragment_to_addCarFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        startCarsListener()
    }

    override fun onStop() {
        carsListener?.remove()
        carsListener = null
        super.onStop()
    }

    override fun onDestroyView() {
        carList = null
        super.onDestroyView()
    }

    private fun startCarsListener() {
        carsListener?.remove()
        carsListener = carRepository.observeCurrentOwnerCars { result ->
            if (!isAdded) return@observeCurrentOwnerCars

            result
                .onSuccess { cars ->
                    showCars(cars)
                }
                .onFailure {
                    showMessage(R.string.load_cars_failed)
                }
        }
    }

    private fun showCars(cars: List<CarListItem>) {
        val list = carList ?: return
        list.removeAllViews()

        if (cars.isEmpty()) {
            list.addView(createEmptyView())
            return
        }

        cars.forEachIndexed { index, car ->
            list.addView(createCarRow(car))
            if (index < cars.lastIndex) {
                list.addView(createDivider())
            }
        }
    }

    private fun createCarRow(car: CarListItem): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            foreground = attributes.getDrawable(0)
            attributes.recycle()
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener {
                AppAnalytics.carOpened(requireContext())
                findNavController().navigate(
                    R.id.action_ownerFragment_to_carDetailFragment,
                    Bundle().apply {
                        putString(ARG_CAR_ID, car.id)
                    }
                )
            }

            addView(createTextView("${car.manufacturer} ${car.model}", 18f, true))
            addView(createTextView(car.licensePlate, 15f, false, topMarginDp = 6))
            addView(createTextView(car.vin, 14f, false, topMarginDp = 4))
            addView(createTextView(worksheetsText(car.worksheetCount), 14f, false, topMarginDp = 4))
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
            text = getString(R.string.no_cars_yet)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(36)
            }
        }
    }

    private fun worksheetsText(count: Int): String {
        return resources.getQuantityString(R.plurals.worksheet_count, count, count)
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ARG_CAR_ID = "carId"
    }
}
