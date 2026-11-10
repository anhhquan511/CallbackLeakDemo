package com.example.callbackleakdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class ReceiverDemoActivity : BaseDemoActivity() {
    override val caseTitle = "A · BroadcastReceiver"
    override val explanation = "Receiver đăng ký bằng applicationContext nhưng gọi vào Activity. " +
        "Bản lỗi quên unregister; bản sửa unregister trong onStop. " +
        "Application Context không cứu được callback vẫn giữ Activity."

    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onDemoEvent("Receiver", intent.getLongExtra("tick", 0L))
        }
    }

    override fun onStart() {
        super.onStart()
        if (!registered) {
            ContextCompat.registerReceiver(applicationContext, receiver,
                IntentFilter(DemoApplication.ACTION_TICK), ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            Metrics.receivers++
        }
    }

    override fun onStop() {
        if (fixed && registered) {
            applicationContext.unregisterReceiver(receiver)
            registered = false
            Metrics.receivers--
        }
        super.onStop()
    }
}
