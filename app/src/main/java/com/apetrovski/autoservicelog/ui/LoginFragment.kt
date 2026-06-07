package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.auth.AuthRepository

class LoginFragment : Fragment(R.layout.screen_login) {
    private val authRepository = AuthRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.loginEmailInput)
        val passwordInput = view.findViewById<EditText>(R.id.loginPasswordInput)
        val loginButton = view.findViewById<View>(R.id.loginButton)
        val openCreateAccountButton = view.findViewById<View>(R.id.openCreateAccountButton)
        val navController = findNavController()

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isBlank() || password.isBlank()) {
                showMessage(R.string.auth_fill_all_fields)
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            openCreateAccountButton.isEnabled = false
            authRepository.loginWithEmail(email, password) { result ->
                if (!isAdded) return@loginWithEmail
                if (navController.currentDestination?.id != R.id.loginFragment) {
                    return@loginWithEmail
                }

                loginButton.isEnabled = true
                openCreateAccountButton.isEnabled = true
                result
                    .onSuccess { profile ->
                        when (profile.role) {
                            AuthRepository.ROLE_OWNER -> {
                                navController.navigate(R.id.action_loginFragment_to_ownerFragment)
                            }
                            AuthRepository.ROLE_MECHANIC -> {
                                navController.navigate(R.id.action_loginFragment_to_mechanicFragment)
                            }
                            else -> showMessage(R.string.auth_unknown_role)
                        }
                    }
                    .onFailure {
                        showMessage(R.string.login_failed)
                    }
            }
        }

        openCreateAccountButton.setOnClickListener {
            navController.navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }
}
