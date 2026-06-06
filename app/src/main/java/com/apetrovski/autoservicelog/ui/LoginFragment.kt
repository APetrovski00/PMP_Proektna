package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R

class LoginFragment : Fragment(R.layout.screen_login) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.loginButton).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_ownerFragment)
        }
        view.findViewById<View>(R.id.openCreateAccountButton).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }
}
