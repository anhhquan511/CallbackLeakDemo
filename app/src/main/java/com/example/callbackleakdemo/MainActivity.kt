package com.example.callbackleakdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var stats: TextView
    private val refresh = object : Runnable {
        override fun run() {
            stats.text = "ĐĂNG KÝ ĐANG TỒN TẠI\n" +
                "Receiver: ${Metrics.receivers}\n" +
                "Listener trong Singleton: ${EventHub.listenerCount}\n" +
                "Service binding: ${Metrics.bindings}\n" +
                "Listener trong Service: ${Metrics.serviceListeners}\n" +
                "Service instance: ${Metrics.serviceInstances}\n\n" +
                "Callback gọi vào Activity đã hủy: ${Metrics.staleCalls}\n\n" +
                "Các số này là bộ đếm của demo, không phải kết quả heap analysis."
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = demoColumn()
        column.note("Callback & tài nguyên hệ thống", 24f)
        column.note("Mục 2.1.2.3 • Chọn một trường hợp và một chế độ.")
        val fixedSwitch = Switch(this).apply {
            text = "Bật = ĐÃ SỬA; tắt = CỐ Ý GÂY LEAK"
            isChecked = true
        }
        column.addView(fixedSwitch)
        fun open(target: Class<out Activity>) {
            startActivity(Intent(this, target)
                .putExtra(BaseDemoActivity.EXTRA_FIXED, fixedSwitch.isChecked))
        }
        column.action("A · BroadcastReceiver") { open(ReceiverDemoActivity::class.java) }
        column.action("B · Callback / Listener") { open(ListenerDemoActivity::class.java) }
        column.action("C · Bound Service") { open(ServiceDemoActivity::class.java) }
        stats = column.note("")
        column.note("QUY TRÌNH\n1. Chọn chế độ và mở demo.\n" +
            "2. Bấm Tạo lại màn hình 5–10 lần.\n3. Trở về đây để xem bộ đếm.\n" +
            "4. Xem thông báo/biểu tượng Leaks và Logcat.\n\n" +
            "Trước khi so sánh chế độ khác, Force stop ứng dụng rồi mở lại. " +
            "Chuyển công tắc KHÔNG dọn những leak đã tạo ở lượt trước.")
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }
}
