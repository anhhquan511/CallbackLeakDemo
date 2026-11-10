package com.example.callbackleakdemo

import leakcanary.LeakCanary

class DebugDemoApplication : DemoApplication() {
    override fun onCreate() {
        super.onCreate()
        // Show a leak after one retained destroyed screen, rather than waiting for five.
        LeakCanary.config = LeakCanary.config.copy(retainedVisibleThreshold = 1)
    }
}
