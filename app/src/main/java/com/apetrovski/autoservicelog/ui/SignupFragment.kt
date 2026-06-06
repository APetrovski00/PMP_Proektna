package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class SignupFragment : Fragment(R.layout.screen_signup) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ownerCheckBox = view.findViewById<CheckBox>(R.id.ownerCheckBox)
        val mechanicCheckBox = view.findViewById<CheckBox>(R.id.mechanicCheckBox)

        ownerCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) mechanicCheckBox.isChecked = false
        }
        mechanicCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) ownerCheckBox.isChecked = false
        }

        view.findViewById<View>(R.id.createAccountButton).setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_welcomeFragment)
        }
        view.findViewById<View>(R.id.backToLoginButton).setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_welcomeFragment)
        }
    }
}
