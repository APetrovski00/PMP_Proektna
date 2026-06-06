package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class CarDetailFragment : Fragment(R.layout.screen_car_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.worksheetOne).setOnClickListener {
            findNavController().navigate(R.id.action_carDetailFragment_to_worksheetDetailFragment)
        }
        view.findViewById<View>(R.id.worksheetTwo).setOnClickListener {
            findNavController().navigate(R.id.action_carDetailFragment_to_worksheetDetailFragment)
        }
    }
}
