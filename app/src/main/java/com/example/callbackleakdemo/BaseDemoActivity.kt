package com.example.callbackleakdemo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

abstract class BaseDemoActivity : Activity() {
    protected val fixed: Boolean by lazy { intent.getBooleanExtra(EXTRA_FIXED, true) }
    protected val instanceId = ++Metrics.nextId
    private var destroyedForDemo = false
    private var events = 0
    // One clearly documented teaching payload. This is not a real measurement result.
    private val payload = ByteArray(1024 * 1024) { 42 }
    private lateinit var eventView: TextView
    abstract val caseTitle: String
    abstract val explanation: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = demoColumn()
        column.note(caseTitle, 23f)
        column.note("${if (fixed) "ĐÃ SỬA" else "CỐ Ý GÂY LEAK"} · Activity #$instanceId", 20f)
        column.note(explanation)
        column.note("Mỗi Activity giữ 1 MiB dữ liệu minh họa.\n" +
            "Nút dưới gọi recreate(): hủy instance cũ và tạo instance mới, " +
            "tương tự phần vòng đời cần quan sát khi xoay màn hình.")
        eventView = column.note("Đang chờ sự kiện...")
        column.action("Tạo lại màn hình") { recreate() }
        column.action("Đóng màn hình / về bảng theo dõi") { finish() }
        column.note("Logcat: tag CallbackDemo.\n" +
            "Run app bình thường; tránh gắn debugger khi chờ LeakCanary.\n" +
            "Sau khi tạo lại, chờ khoảng 15–60 giây để công cụ phân tích; " +
            "thời gian thực tế có thể khác.")
        Log.i(TAG, "CREATE ${javaClass.simpleName} #$instanceId fixed=$fixed payload=${payload.size}")
    }

    protected fun onDemoEvent(source: String, value: Long) {
        // This instance-method invocation is the intentional Activity capture.
        if (destroyedForDemo) {
            Metrics.staleCalls++
            Log.w(TAG, "STALE_CALLBACK $source -> DESTROYED Activity #$instanceId tick=$value")
            return
        }
        events++
        eventView.text = "Activity #$instanceId nhận $events sự kiện\nNguồn: $source · tick=$value"
    }

    override fun onDestroy() {
        destroyedForDemo = true
        Log.i(TAG, "DESTROY ${javaClass.simpleName} #$instanceId fixed=$fixed")
        super.onDestroy()
    }

    companion object {
        const val EXTRA_FIXED = "fixed"
        const val TAG = "CallbackDemo"
    }
}
