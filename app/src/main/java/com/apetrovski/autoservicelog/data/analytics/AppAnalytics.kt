package com.apetrovski.autoservicelog.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AppAnalytics {
    fun loginSuccess(context: Context, method: String, role: String) {
        logEvent(
            context,
            "login_success",
            "method" to method,
            "role" to role
        )
    }

    fun accountCreated(context: Context, role: String) {
        logEvent(
            context,
            "account_created",
            "role" to role
        )
    }

    fun carAdded(context: Context) {
        logEvent(context, "car_added")
    }

    fun carOpened(context: Context) {
        logEvent(context, "car_opened")
    }

    fun carSearch(context: Context, found: Boolean) {
        logEvent(
            context,
            "car_search",
            "found" to found.toString()
        )
    }

    fun workStarted(context: Context) {
        logEvent(context, "work_started")
    }

    fun workSaved(context: Context, hasPhoto: Boolean) {
        logEvent(
            context,
            "work_saved",
            "has_photo" to hasPhoto.toString()
        )
    }

    fun workFinished(context: Context) {
        logEvent(context, "work_finished")
    }

    fun worksheetOpened(context: Context) {
        logEvent(context, "worksheet_opened")
    }

    private fun logEvent(
        context: Context,
        name: String,
        vararg params: Pair<String, String>
    ) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            bundle.putString(key, value)
        }
        FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
    }
}
