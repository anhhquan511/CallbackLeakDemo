package com.example.callbackleakdemo

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class TickService : Service() {
    inner class LocalBinder : Binder() {
        fun service(): TickService = this@TickService
    }
    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = linkedSetOf<(Long) -> Unit>()
    private var tick = 0L
    private val ticker = object : Runnable {
        override fun run() {
            tick++
            listeners.toList().forEach { it(tick) }
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Metrics.serviceInstances++
        Log.i(BaseDemoActivity.TAG, "SERVICE CREATE")
        handler.postDelayed(ticker, 1000L)
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun addListener(listener: (Long) -> Unit) {
        if (listeners.add(listener)) Metrics.serviceListeners++
    }

    fun removeListener(listener: (Long) -> Unit) {
        if (listeners.remove(listener)) Metrics.serviceListeners--
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        Metrics.serviceListeners -= listeners.size
        listeners.clear()
        Metrics.serviceInstances--
        Log.i(BaseDemoActivity.TAG, "SERVICE DESTROY")
        super.onDestroy()
    }
}
