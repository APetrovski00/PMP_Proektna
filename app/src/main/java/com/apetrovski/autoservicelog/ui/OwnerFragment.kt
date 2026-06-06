package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class OwnerFragment : Fragment(R.layout.screen_owner) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.addCarButton).setOnClickListener {
            findNavController().navigate(R.id.action_ownerFragment_to_addCarFragment)
        }
        view.findViewById<View>(R.id.carRowOne).setOnClickListener {
            findNavController().navigate(R.id.action_ownerFragment_to_carDetailFragment)
        }
        view.findViewById<View>(R.id.carRowTwo).setOnClickListener {
            findNavController().navigate(R.id.action_ownerFragment_to_carDetailFragment)
        }
    }
}
