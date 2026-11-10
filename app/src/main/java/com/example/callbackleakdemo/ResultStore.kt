package com.example.callbackleakdemo

import android.content.Context
import android.content.SharedPreferences

/**
 * Lưu kết quả đo vào SharedPreferences để so sánh giữa hai lượt chạy (lỗi vs sửa).
 * Dữ liệu tồn tại qua Force Stop.
 */
object ResultStore {
    private const val PREFS = "demo_results"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Lưu snapshot hiện tại cho một chế độ ("leak" hoặc "fixed"). */
    fun save(ctx: Context, mode: String, case: String,
             receivers: Int, listeners: Int, bindings: Int,
             serviceListeners: Int, serviceInstances: Int, staleCalls: Int) {
        prefs(ctx).edit()
            .putString("${mode}_case", case)
            .putInt("${mode}_receivers", receivers)
            .putInt("${mode}_listeners", listeners)
            .putInt("${mode}_bindings", bindings)
            .putInt("${mode}_serviceListeners", serviceListeners)
            .putInt("${mode}_serviceInstances", serviceInstances)
            .putInt("${mode}_staleCalls", staleCalls)
            .putLong("${mode}_time", System.currentTimeMillis())
            .apply()
    }

    /** Đọc kết quả đã lưu. Trả về null nếu chưa có. */
    fun load(ctx: Context, mode: String): Map<String, Any>? {
        val p = prefs(ctx)
        if (!p.contains("${mode}_time")) return null
        return mapOf(
            "case" to (p.getString("${mode}_case", "") ?: ""),
            "receivers" to p.getInt("${mode}_receivers", 0),
            "listeners" to p.getInt("${mode}_listeners", 0),
            "bindings" to p.getInt("${mode}_bindings", 0),
            "serviceListeners" to p.getInt("${mode}_serviceListeners", 0),
            "serviceInstances" to p.getInt("${mode}_serviceInstances", 0),
            "staleCalls" to p.getInt("${mode}_staleCalls", 0),
            "time" to p.getLong("${mode}_time", 0L)
        )
    }

    fun hasBothResults(ctx: Context): Boolean =
        prefs(ctx).contains("leak_time") && prefs(ctx).contains("fixed_time")

    fun clear(ctx: Context) = prefs(ctx).edit().clear().apply()
}
