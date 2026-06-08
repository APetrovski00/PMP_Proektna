package com.apetrovski.autoservicelog

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.apetrovski.autoservicelog.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var navController: NavController? = null
    private var currentUserId: String? = null
    private var currentRole: String? = null
    private var currentDisplayName: String? = null
    private var headerProfileLoaded: Boolean = false
    private var lastDestinationId: Int = 0
    private var lastShowJobsArgument: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController
        setupHeaderTabs()
        navHost.navController.addOnDestinationChangedListener { _, destination, arguments ->
            lastDestinationId = destination.id
            lastShowJobsArgument = arguments?.getBoolean(ARG_SHOW_JOBS, false) ?: false
            refreshHeaderUser()
            updateHeader()
        }
    }

    private fun setupHeaderTabs() {
        findViewById<TextView>(R.id.myCarsTab).setOnClickListener {
            navigateToSection(R.id.ownerFragment)
        }
        findViewById<TextView>(R.id.jobsTab).setOnClickListener {
            navigateToSection(R.id.mechanicFragment)
        }
        findViewById<TextView>(R.id.profileTab).setOnClickListener {
            val controller = navController ?: return@setOnClickListener
            if (controller.currentDestination?.id == R.id.profileFragment) return@setOnClickListener

            controller.navigate(
                R.id.profileFragment,
                Bundle().apply {
                    putBoolean(ARG_SHOW_JOBS, shouldShowJobs())
                }
            )
        }
    }

    private fun navigateToSection(destinationId: Int) {
        val controller = navController ?: return
        if (controller.currentDestination?.id == destinationId) return

        if (!controller.popBackStack(destinationId, false)) {
            controller.navigate(destinationId)
        }
    }

    private fun refreshHeaderUser() {
        val user = auth.currentUser
        if (user == null) {
            currentUserId = null
            currentRole = null
            currentDisplayName = null
            headerProfileLoaded = false
            return
        }

        if (currentUserId != user.uid) {
            currentUserId = user.uid
            currentRole = null
            currentDisplayName = null
            headerProfileLoaded = false
        }

        if (headerProfileLoaded) return
        currentRole = if (user.isAnonymous) AuthRepository.ROLE_ANONYMOUS else null

        if (user.isAnonymous) {
            currentDisplayName = getString(R.string.anonymous_user)
            headerProfileLoaded = true
        } else {
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    currentRole = document.getString("role")
                    currentDisplayName = document.getString("displayName")
                    headerProfileLoaded = true
                    updateHeader()
                }
        }
    }

    private fun updateHeader() {
        val header = findViewById<View>(R.id.mainHeader)
        if (!shouldShowHeader()) {
            header.visibility = View.GONE
            return
        }

        header.visibility = View.VISIBLE
        findViewById<TextView>(R.id.headerUserNameText).text = currentUserName()

        val showJobs = shouldShowJobs()
        findViewById<View>(R.id.jobsTab).visibility = if (showJobs) View.VISIBLE else View.GONE
        findViewById<View>(R.id.jobsSeparator).visibility = if (showJobs) View.VISIBLE else View.GONE
    }

    private fun shouldShowHeader(): Boolean {
        return lastDestinationId == R.id.ownerFragment ||
            lastDestinationId == R.id.addCarFragment ||
            lastDestinationId == R.id.mechanicFragment ||
            lastDestinationId == R.id.carDetailFragment ||
            lastDestinationId == R.id.worksheetDetailFragment ||
            lastDestinationId == R.id.profileFragment
    }

    private fun shouldShowJobs(): Boolean {
        return lastShowJobsArgument ||
            lastDestinationId == R.id.mechanicFragment ||
            currentRole == AuthRepository.ROLE_MECHANIC
    }

    private fun currentUserName(): String {
        val user = auth.currentUser ?: return getString(R.string.anonymous_user)
        if (user.isAnonymous) return getString(R.string.anonymous_user)

        return AuthRepository.formatProfileName(
            currentDisplayName ?: user.displayName,
            user.email.orEmpty()
        )
    }

    companion object {
        private const val ARG_SHOW_JOBS = "showJobs"
        private const val USERS_COLLECTION = "users"
    }
}
