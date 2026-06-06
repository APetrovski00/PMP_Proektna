package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class MechanicFragment : Fragment(R.layout.screen_mechanic) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.startWorkButton).setOnClickListener {
            findNavController().navigate(R.id.action_mechanicFragment_to_worksheetFragment)
        }
    }
}
