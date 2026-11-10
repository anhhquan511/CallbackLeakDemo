package com.example.callbackleakdemo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

class ServiceDemoActivity : BaseDemoActivity() {
    override val caseTitle = "C · Bound Service"
    override val explanation = "Pure Bound Service: không gọi startService. " +
        "ServiceConnection và listener có thể giữ Activity. Bản lỗi quên remove/unbind; " +
        "bản sửa làm cả hai trong onStop. Service dừng khi client cuối cùng unbind."

    private var bindingRequested = false
    private var service: TickService? = null
    private val listener: (Long) -> Unit = { tick -> onDemoEvent("Bound Service", tick) }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as TickService.LocalBinder).service()
            service?.addListener(listener)
            onDemoEvent("ServiceConnection", 0L)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // The binding is still registered; Android may reconnect it.
            service = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (!bindingRequested) {
            // Same context for bind and unbind. Intentional app scope keeps the bad
            // sample reproducible without relying on Activity context cleanup.
            bindingRequested = applicationContext.bindService(
                Intent(this, TickService::class.java), connection, Context.BIND_AUTO_CREATE)
            if (bindingRequested) {
                Metrics.bindings++
            } else {
                // Some failed bind paths still create a dispatcher registration.
                runCatching { applicationContext.unbindService(connection) }
                Log.e(TAG, "bindService returned false; do not count this run")
            }
        }
    }

    override fun onStop() {
        if (fixed && bindingRequested) {
            service?.removeListener(listener)
            service = null
            // Do not depend on onServiceConnected having run before releasing.
            applicationContext.unbindService(connection)
            bindingRequested = false
            Metrics.bindings--
        }
        super.onStop()
    }
}
