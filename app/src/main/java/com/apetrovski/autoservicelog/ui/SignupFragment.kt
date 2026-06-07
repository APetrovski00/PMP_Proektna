package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.auth.AuthRepository

class SignupFragment : Fragment(R.layout.screen_signup) {
    private val authRepository = AuthRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.signupEmailInput)
        val passwordInput = view.findViewById<EditText>(R.id.signupPasswordInput)
        val confirmPasswordInput = view.findViewById<EditText>(R.id.signupConfirmPasswordInput)
        val ownerCheckBox = view.findViewById<CheckBox>(R.id.ownerCheckBox)
        val mechanicCheckBox = view.findViewById<CheckBox>(R.id.mechanicCheckBox)
        val createAccountButton = view.findViewById<View>(R.id.createAccountButton)
        val backToLoginButton = view.findViewById<View>(R.id.backToLoginButton)
        val navController = findNavController()

        ownerCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) mechanicCheckBox.isChecked = false
        }
        mechanicCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) ownerCheckBox.isChecked = false
        }

        createAccountButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val selectedRole = when {
                ownerCheckBox.isChecked -> AuthRepository.ROLE_OWNER
                mechanicCheckBox.isChecked -> AuthRepository.ROLE_MECHANIC
                else -> null
            }

            when {
                email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                    showMessage(R.string.auth_fill_all_fields)
                }
                password != confirmPassword -> {
                    showMessage(R.string.auth_passwords_do_not_match)
                }
                selectedRole == null -> {
                    showMessage(R.string.auth_choose_account_type)
                }
                else -> {
                    createAccountButton.isEnabled = false
                    backToLoginButton.isEnabled = false
                    authRepository.createAccount(email, password, selectedRole) { result ->
                        if (!isAdded) return@createAccount
                        if (navController.currentDestination?.id != R.id.signupFragment) {
                            return@createAccount
                        }

                        createAccountButton.isEnabled = true
                        backToLoginButton.isEnabled = true
                        result
                            .onSuccess {
                                authRepository.logout()
                                showMessage(R.string.account_created_notification)
                                navController.navigate(R.id.action_signupFragment_to_welcomeFragment)
                            }
                            .onFailure {
                                showMessage(R.string.signup_failed)
                            }
                    }
                }
            }
        }

        backToLoginButton.setOnClickListener {
            navController.navigate(R.id.action_signupFragment_to_welcomeFragment)
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }
}
