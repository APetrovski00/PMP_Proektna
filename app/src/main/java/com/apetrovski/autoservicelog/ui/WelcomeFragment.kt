package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.analytics.AppAnalytics
import com.apetrovski.autoservicelog.data.auth.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment(R.layout.screen_welcome) {
    private val authRepository = AuthRepository()
    private lateinit var credentialManager: CredentialManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        credentialManager = CredentialManager.create(requireContext())

        val navController = findNavController()
        val emailLoginButton = view.findViewById<View>(R.id.emailLoginButton)
        val googleLoginButton = view.findViewById<View>(R.id.googleLoginButton)
        val facebookLoginButton = view.findViewById<View>(R.id.facebookLoginButton)
        val signupButton = view.findViewById<View>(R.id.signupButton)
        val authButtons = listOf(emailLoginButton, googleLoginButton, facebookLoginButton, signupButton)

        emailLoginButton.setOnClickListener {
            navController.navigate(R.id.action_welcomeFragment_to_loginFragment)
        }
        googleLoginButton.setOnClickListener {
            startGoogleLogin(authButtons)
        }
        signupButton.setOnClickListener {
            navController.navigate(R.id.action_welcomeFragment_to_signupFragment)
        }
    }

    private fun startGoogleLogin(authButtons: List<View>) {
        val webClientId = googleWebClientId()
        if (webClientId == null) {
            showMessage(R.string.google_config_missing)
            return
        }

        setButtonsEnabled(authButtons, false)
        viewLifecycleOwner.lifecycleScope.launch {
            val idToken = getGoogleIdToken(webClientId)
            if (!isAdded) return@launch
            if (findNavController().currentDestination?.id != R.id.welcomeFragment) return@launch

            if (idToken == null) {
                setButtonsEnabled(authButtons, true)
                showMessage(R.string.google_login_failed)
                return@launch
            }

            authRepository.loginWithGoogle(idToken) { result ->
                if (!isAdded) return@loginWithGoogle
                if (findNavController().currentDestination?.id != R.id.welcomeFragment) return@loginWithGoogle

                setButtonsEnabled(authButtons, true)
                result
                    .onSuccess { profile ->
                        if (profile == null) {
                            showGoogleRoleDialog()
                        } else {
                            AppAnalytics.loginSuccess(requireContext(), "google", profile.role)
                            navigateForRole(profile.role)
                        }
                    }
                    .onFailure {
                        showMessage(R.string.google_login_failed)
                    }
            }
        }
    }

    private suspend fun getGoogleIdToken(webClientId: String): String? {
        val credential = runCatching {
            getGoogleCredential(webClientId, filterByAuthorizedAccounts = true)
        }.getOrElse {
            runCatching {
                getGoogleCredential(webClientId, filterByAuthorizedAccounts = false)
            }.getOrElse {
                null
            }
        }

        return readGoogleIdToken(credential)
    }

    private suspend fun getGoogleCredential(
        webClientId: String,
        filterByAuthorizedAccounts: Boolean
    ): Credential {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return credentialManager.getCredential(requireContext(), request).credential
    }

    private fun readGoogleIdToken(credential: Credential?): String? {
        if (credential !is CustomCredential) {
            return null
        }
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return null
        }

        return runCatching {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        }.getOrElse {
            null
        }
    }

    private fun showGoogleRoleDialog() {
        val ownerCheckbox = CheckBox(requireContext()).apply {
            text = getString(R.string.owner)
        }
        val mechanicCheckbox = CheckBox(requireContext()).apply {
            text = getString(R.string.mechanic_role)
        }

        val roleRow = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            addView(
                ownerCheckbox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(24)
                }
            )
            addView(
                mechanicCheckbox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialogContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
            addView(
                TextView(requireContext()).apply {
                    text = getString(R.string.google_account_type_message)
                    textSize = 16f
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(16)
                }
            )
            addView(
                roleRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.account_type)
            .setView(dialogContent)
            .setPositiveButton(R.string.continue_action, null)
            .setOnCancelListener {
                authRepository.logout()
            }
            .create()

        dialog.setOnShowListener {
            val continueButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            fun updateContinueButton() {
                continueButton.isEnabled = ownerCheckbox.isChecked || mechanicCheckbox.isChecked
            }

            ownerCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) mechanicCheckbox.isChecked = false
                updateContinueButton()
            }
            mechanicCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) ownerCheckbox.isChecked = false
                updateContinueButton()
            }
            continueButton.setOnClickListener {
                when {
                    ownerCheckbox.isChecked -> {
                        dialog.dismiss()
                        saveGoogleRole(AuthRepository.ROLE_OWNER)
                    }
                    mechanicCheckbox.isChecked -> {
                        dialog.dismiss()
                        saveGoogleRole(AuthRepository.ROLE_MECHANIC)
                    }
                    else -> showMessage(R.string.auth_choose_account_type)
                }
            }
            updateContinueButton()
        }

        dialog.show()
    }

    private fun saveGoogleRole(role: String) {
        authRepository.saveCurrentUserRole(role) { result ->
            if (!isAdded) return@saveCurrentUserRole
            if (findNavController().currentDestination?.id != R.id.welcomeFragment) return@saveCurrentUserRole

            result
                .onSuccess { profile ->
                    AppAnalytics.accountCreated(requireContext(), profile.role)
                    AppAnalytics.loginSuccess(requireContext(), "google", profile.role)
                    navigateForRole(profile.role)
                }
                .onFailure {
                    authRepository.logout()
                    showMessage(R.string.google_login_failed)
                }
        }
    }

    private fun navigateForRole(role: String) {
        val navController = findNavController()
        when (role) {
            AuthRepository.ROLE_OWNER -> navController.navigate(R.id.action_welcomeFragment_to_ownerFragment)
            AuthRepository.ROLE_MECHANIC -> navController.navigate(R.id.action_welcomeFragment_to_mechanicFragment)
            else -> showMessage(R.string.auth_unknown_role)
        }
    }

    private fun googleWebClientId(): String? {
        val resourceId = resources.getIdentifier(
            "default_web_client_id",
            "string",
            requireContext().packageName
        )

        if (resourceId == 0) return null
        return getString(resourceId)
    }

    private fun setButtonsEnabled(buttons: List<View>, enabled: Boolean) {
        buttons.forEach { button ->
            button.isEnabled = enabled
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

}
