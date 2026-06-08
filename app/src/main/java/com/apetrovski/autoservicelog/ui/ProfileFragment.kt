package com.apetrovski.autoservicelog.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.apetrovski.autoservicelog.R
import com.apetrovski.autoservicelog.data.auth.AuthRepository
import java.util.Locale

class ProfileFragment : Fragment(R.layout.screen_profile) {
    private val authRepository = AuthRepository()

    private var profileNameText: TextView? = null
    private var profileEmailText: TextView? = null
    private var profileRoleText: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileNameText = view.findViewById(R.id.profileNameText)
        profileEmailText = view.findViewById(R.id.profileEmailText)
        profileRoleText = view.findViewById(R.id.profileRoleText)

        view.findViewById<View>(R.id.logoutButton).setOnClickListener {
            authRepository.logout()
            findNavController().navigate(
                R.id.welcomeFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        }

        loadProfile()
    }

    override fun onDestroyView() {
        profileNameText = null
        profileEmailText = null
        profileRoleText = null
        super.onDestroyView()
    }

    private fun loadProfile() {
        authRepository.loadCurrentUserProfile { result ->
            if (!isAdded) return@loadCurrentUserProfile

            result
                .onSuccess { profile ->
                    profileNameText?.text = getString(
                        R.string.profile_name,
                        profile.displayName.ifBlank { getString(R.string.anonymous_user) }
                    )
                    profileEmailText?.text = getString(
                        R.string.profile_email,
                        profile.email.ifBlank { getString(R.string.anonymous_user) }
                    )
                    profileRoleText?.text = getString(
                        R.string.profile_role,
                        profile.role.replaceFirstChar { letter ->
                            letter.titlecase(Locale.getDefault())
                        }
                    )
                }
                .onFailure {
                    showMessage(R.string.auth_unknown_role)
                }
        }
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }
}
