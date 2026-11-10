package com.example.callbackleakdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var stats: TextView
    private lateinit var fixedSwitch: Switch
    private var lastCaseOpened = ""
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
        fixedSwitch = Switch(this).apply {
            text = "Bật = ĐÃ SỬA; tắt = CỐ Ý GÂY LEAK"
            isChecked = true
        }
        column.addView(fixedSwitch)
        fun open(target: Class<out Activity>, caseName: String) {
            lastCaseOpened = caseName
            startActivity(Intent(this, target)
                .putExtra(BaseDemoActivity.EXTRA_FIXED, fixedSwitch.isChecked))
        }
        column.action("A · BroadcastReceiver") { open(ReceiverDemoActivity::class.java, "Receiver") }
        column.action("B · Callback / Listener") { open(ListenerDemoActivity::class.java, "Listener") }
        column.action("C · Bound Service") { open(ServiceDemoActivity::class.java, "Service") }
        stats = column.note("")

        // ── Nút LƯU KẾT QUẢ ──
        column.action("💾 LƯU KẾT QUẢ LƯỢT NÀY") {
            val mode = if (fixedSwitch.isChecked) "fixed" else "leak"
            val modeLabel = if (fixedSwitch.isChecked) "ĐÃ SỬA" else "CỐ Ý GÂY LEAK"
            val caseName = if (lastCaseOpened.isNotEmpty()) lastCaseOpened else "Chưa chọn"
            ResultStore.save(
                ctx = this, mode = mode, case = caseName,
                receivers = Metrics.receivers,
                listeners = EventHub.listenerCount,
                bindings = Metrics.bindings,
                serviceListeners = Metrics.serviceListeners,
                serviceInstances = Metrics.serviceInstances,
                staleCalls = Metrics.staleCalls
            )
            Toast.makeText(this,
                "Đã lưu kết quả lượt $modeLabel (ca $caseName)", Toast.LENGTH_LONG).show()
        }

        // ── Nút SO SÁNH ──
        column.action("📊 SO SÁNH TRƯỚC / SAU") {
            startActivity(Intent(this, ComparisonActivity::class.java))
        }

        column.note("QUY TRÌNH\n1. Chọn chế độ và mở demo.\n" +
            "2. Bấm Tạo lại màn hình 5–10 lần.\n3. Trở về đây → bấm LƯU KẾT QUẢ.\n" +
            "4. Force stop → mở lại → đổi chế độ → lặp lại.\n" +
            "5. Bấm SO SÁNH TRƯỚC/SAU để xem hai lượt cạnh nhau.\n\n" +
            "Kết quả lưu qua SharedPreferences, tồn tại qua Force Stop.")
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
