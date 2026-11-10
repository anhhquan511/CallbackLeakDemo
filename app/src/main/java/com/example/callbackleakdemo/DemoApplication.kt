package com.example.callbackleakdemo

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

open class DemoApplication : Application() {
    private val handler = Handler(Looper.getMainLooper())
    private var startedActivities = 0
    private var tick = 0L

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { startedActivities++ }
            override fun onActivityStopped(activity: Activity) { startedActivities-- }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        // This demo ticker owns only Application state, never an Activity or View.
        handler.post(object : Runnable {
            override fun run() {
                if (startedActivities > 0) {
                    tick++
                    EventHub.emit(tick)
                    sendBroadcast(Intent(ACTION_TICK).setPackage(packageName)
                        .putExtra("tick", tick))
                }
                handler.postDelayed(this, 1000L)
            }
        })
    }

    companion object {
        const val ACTION_TICK = "com.example.callbackleakdemo.TICK"
    }
}
