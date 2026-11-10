package com.lexapps.sblgntreader

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SblgntApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Public checkouts work without connecting to the maintainer's Firebase project.
        FirebaseApp.initializeApp(this)?.let {
            val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !isDebuggable
        }
    }
}


