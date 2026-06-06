package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class AddCarFragment : Fragment(R.layout.screen_add_car) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.cancelAddCarButton).setOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<View>(R.id.saveCarButton).setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
