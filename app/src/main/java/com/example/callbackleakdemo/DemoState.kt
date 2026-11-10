package com.example.callbackleakdemo

// All access is on the main thread. No Activity references are stored in Metrics.
object Metrics {
    var nextId = 0
    var receivers = 0
    var bindings = 0
    var serviceListeners = 0
    var serviceInstances = 0
    var staleCalls = 0
}

object EventHub {
    private val listeners = linkedSetOf<(Long) -> Unit>()
    val listenerCount: Int get() = listeners.size
    fun add(listener: (Long) -> Unit) { listeners.add(listener) }
    fun remove(listener: (Long) -> Unit) { listeners.remove(listener) }
    fun emit(tick: Long) { listeners.toList().forEach { it(tick) } }
}
