package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class WelcomeFragment : Fragment(R.layout.screen_welcome) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.emailLoginButton).setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }
        view.findViewById<View>(R.id.signupButton).setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_signupFragment)
        }
    }
}
