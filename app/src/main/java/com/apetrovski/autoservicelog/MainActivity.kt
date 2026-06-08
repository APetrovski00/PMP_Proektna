package com.apetrovski.autoservicelog

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.apetrovski.autoservicelog.data.auth.AuthRepository
import com.apetrovski.autoservicelog.data.messaging.MessagingRepository
import com.apetrovski.autoservicelog.data.messaging.WorkNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val messagingRepository = MessagingRepository()
    private var navController: NavController? = null
    private var currentUserId: String? = null
    private var currentRole: String? = null
    private var currentDisplayName: String? = null
    private var lastMessagingUserId: String? = null
    private var lastNotificationUserId: String? = null
    private var notificationListener: ListenerRegistration? = null
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
        createNotificationChannel()
        requestNotificationPermission()
        navHost.navController.addOnDestinationChangedListener { _, destination, arguments ->
            lastDestinationId = destination.id
            lastShowJobsArgument = arguments?.getBoolean(ARG_SHOW_JOBS, false) ?: false
            refreshHeaderUser()
            saveMessagingToken()
            startNotificationListener()
            updateHeader()
        }
    }

    override fun onDestroy() {
        notificationListener?.remove()
        notificationListener = null
        super.onDestroy()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun saveMessagingToken() {
        val user = auth.currentUser
        if (user == null) {
            lastMessagingUserId = null
            return
        }
        if (lastMessagingUserId == user.uid) return

        lastMessagingUserId = user.uid
        messagingRepository.saveCurrentToken()
    }

    private fun startNotificationListener() {
        val user = auth.currentUser
        if (user == null) {
            lastNotificationUserId = null
            notificationListener?.remove()
            notificationListener = null
            return
        }
        if (lastNotificationUserId == user.uid) return

        lastNotificationUserId = user.uid
        notificationListener?.remove()
        notificationListener = messagingRepository.observePendingWorkNotifications { result ->
            result
                .onSuccess { notifications ->
                    notifications.forEach { notification ->
                        showWorkStartedNotification(notification)
                        messagingRepository.markNotificationDelivered(notification.id)
                    }
                }
        }
    }

    private fun showWorkStartedNotification(notification: WorkNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val carName = listOf(notification.manufacturer, notification.model)
            .filter { value -> value.isNotBlank() }
            .joinToString(" ")
            .ifBlank { notification.licensePlate }
        val mechanicName = notification.mechanicName.ifBlank { getString(R.string.mechanic) }
        val body = getString(R.string.notification_work_started_owner_body, carName, mechanicName)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val androidNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_work_started_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notification.id.hashCode(), androidNotification)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST
        )
    }

    companion object {
        private const val ARG_SHOW_JOBS = "showJobs"
        private const val USERS_COLLECTION = "users"
        private const val NOTIFICATION_PERMISSION_REQUEST = 2001
        private const val NOTIFICATION_CHANNEL_ID = "work_updates"
    }
}
