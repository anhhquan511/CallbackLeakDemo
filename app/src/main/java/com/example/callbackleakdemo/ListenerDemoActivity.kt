package com.example.callbackleakdemo

class ListenerDemoActivity : BaseDemoActivity() {
    override val caseTitle = "B · Callback / Listener"
    override val explanation = "EventHub là Singleton giữ một tập listener. " +
        "Lambda này gọi phương thức của Activity. Bản lỗi chỉ add; " +
        "bản sửa remove đúng lambda trong onStop."

    private val listener: (Long) -> Unit = { tick -> onDemoEvent("EventHub", tick) }

    override fun onStart() {
        super.onStart()
        EventHub.add(listener)
    }

    override fun onStop() {
        if (fixed) EventHub.remove(listener)
        super.onStop()
    }
}
