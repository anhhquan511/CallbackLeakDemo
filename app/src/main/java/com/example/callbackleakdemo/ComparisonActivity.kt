package com.example.callbackleakdemo

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn hình so sánh kết quả Trước/Sau tối ưu.
 * Hiển thị hai cột: bản LỖI (đỏ) và bản SỬA (xanh) cạnh nhau.
 */
class ComparisonActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = demoColumn()
        column.note("📊 SO SÁNH KẾT QUẢ", 24f)
        column.note("Trước tối ưu vs. Sau tối ưu")

        val leak = ResultStore.load(this, "leak")
        val fixed = ResultStore.load(this, "fixed")

        if (leak == null && fixed == null) {
            column.note("Chưa có kết quả nào được lưu.\n\n" +
                "Cách sử dụng:\n" +
                "1. TẮT toggle → chạy demo → về dashboard → bấm LƯU KẾT QUẢ\n" +
                "2. Force stop app\n" +
                "3. BẬT toggle → chạy demo → về dashboard → bấm LƯU KẾT QUẢ\n" +
                "4. Mở lại màn hình này để xem so sánh")
            column.action("← Quay lại") { finish() }
            return
        }

        // Header row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 8)
        }
        headerRow.addView(createLabel("CHỈ SỐ", Color.rgb(100, 100, 100), true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        })
        headerRow.addView(createLabel("❌ LỖI", Color.rgb(211, 47, 47), true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
        })
        headerRow.addView(createLabel("✅ SỬA", Color.rgb(46, 125, 50), true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
        })
        column.addView(headerRow)

        // Divider
        column.addView(android.view.View(this).apply {
            setBackgroundColor(Color.rgb(200, 200, 200))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2).apply { bottomMargin = 8 }
        })

        // Data rows
        val metrics = listOf(
            Triple("Receiver", "receivers", "receivers"),
            Triple("Listener Singleton", "listeners", "listeners"),
            Triple("Service binding", "bindings", "bindings"),
            Triple("Listener Service", "serviceListeners", "serviceListeners"),
            Triple("Service instance", "serviceInstances", "serviceInstances"),
            Triple("Stale callback", "staleCalls", "staleCalls")
        )

        for ((label, key, _) in metrics) {
            val leakVal = (leak?.get(key) as? Int) ?: -1
            val fixVal = (fixed?.get(key) as? Int) ?: -1
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 10)
            }
            row.addView(createLabel(label, Color.rgb(20, 30, 45), false).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            })
            row.addView(createValue(
                if (leak != null) leakVal.toString() else "—",
                if (leak != null && leakVal > 0) Color.rgb(211, 47, 47) else Color.rgb(100, 100, 100)
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            })
            row.addView(createValue(
                if (fixed != null) fixVal.toString() else "—",
                if (fixed != null && fixVal == 0) Color.rgb(46, 125, 50) else Color.rgb(100, 100, 100)
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            })
            column.addView(row)

            // Row divider
            column.addView(android.view.View(this).apply {
                setBackgroundColor(Color.rgb(230, 230, 230))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1)
            })
        }

        // Conclusion box
        val leakStale = (leak?.get("staleCalls") as? Int) ?: 0
        val fixStale = (fixed?.get("staleCalls") as? Int) ?: 0
        val leakTotal = ((leak?.get("receivers") as? Int) ?: 0) +
            ((leak?.get("listeners") as? Int) ?: 0) +
            ((leak?.get("bindings") as? Int) ?: 0)
        val fixTotal = ((fixed?.get("receivers") as? Int) ?: 0) +
            ((fixed?.get("listeners") as? Int) ?: 0) +
            ((fixed?.get("bindings") as? Int) ?: 0)

        if (leak != null && fixed != null) {
            // Conclusion
            val conclusionBg = if (fixTotal == 0 && fixStale == 0)
                Color.rgb(232, 245, 233) else Color.rgb(255, 243, 224)
            val conclusionText = if (fixTotal == 0 && fixStale == 0)
                "✅ KẾT LUẬN: Sau tối ưu, tất cả đăng ký đã được giải phóng đúng.\n" +
                "• Đăng ký tồn đọng: $leakTotal → $fixTotal\n" +
                "• Stale callback: $leakStale → $fixStale\n" +
                "• Activity đã hủy không còn bị giữ lại bởi callback/listener."
            else
                "⚠️ Cần kiểm tra lại: bản sửa vẫn còn đăng ký tồn đọng ($fixTotal) hoặc stale callback ($fixStale)."

            val box = TextView(this).apply {
                text = conclusionText
                textSize = 14f
                setTextColor(Color.rgb(20, 30, 45))
                setBackgroundColor(conclusionBg)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20; bottomMargin = 12 }
            }
            column.addView(box)
        }

        // Timestamps
        val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
        if (leak != null) {
            column.note("Lượt LỖI: ca ${leak["case"]} · ${sdf.format(Date(leak["time"] as Long))}")
        }
        if (fixed != null) {
            column.note("Lượt SỬA: ca ${fixed["case"]} · ${sdf.format(Date(fixed["time"] as Long))}")
        }

        // ── KHUNG GIẢI THÍCH Ý NGHĨA CÁC CHỈ SỐ MINH CHỨNG ──
        val explainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(240, 244, 250))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20; bottomMargin = 16 }
        }

        explainContainer.addView(TextView(this).apply {
            text = "🔍 Ý NGHĨA CÁC CHỈ SỐ MINH CHỨNG LEAK"
            textSize = 16f
            setTextColor(Color.rgb(25, 118, 210))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        })

        val explainItems = listOf(
            "1. Đăng ký tồn đọng (Receiver / Listener / Binding)" to
                "• Bản lỗi: Giữ nguyên > 0 sau khi thoát màn hình (ví dụ: 10 lần recreate → 11 đăng ký).\n" +
                "• Minh chứng: Thành phần sống lâu (Singleton / App Context / Service) duy trì Strong Reference tới Activity đã hủy. Bộ thu gom rác (GC) không thể giải phóng → Gây rò rỉ bộ nhớ.",

            "2. Stale Callback (Gọi vào Activity đã onDestroy)" to
                "• Bản lỗi: Số lượng > 0 và liên tục tăng khi có sự kiện mới.\n" +
                "• Minh chứng: Bằng chứng sống cho thấy đối tượng Activity cũ vẫn tồn tại nguyên vẹn trên Heap RAM và vẫn bị gọi phương thức xử lý dù vòng đời đã kết thúc.",

            "3. Service Instance & Binding (Ca Service)" to
                "• Bản lỗi: Service instance vẫn = 1 sau khi đóng hết màn hình.\n" +
                "• Minh chứng: Quên unbindService() khiến hệ thống không thể tự hủy Pure Bound Service → Rò rỉ tiến trình và tài nguyên hệ thống chạy ngầm.",

            "4. Đối chiếu Công cụ Đo Chuyên sâu (Chương 4)" to
                "• LeakCanary: Bắt Heap Dump và dựng cây tham chiếu (Leak Trace) từ GC Root trỏ tới Activity đã hủy.\n" +
                "• Memory Profiler: Mỗi Activity giữ payload 1 MiB; bản lỗi đồ thị RAM tăng bậc thang liên tục, bản sửa duy trì RAM phẳng ổn định."
        )

        for ((title, content) in explainItems) {
            explainContainer.addView(TextView(this).apply {
                text = title
                textSize = 13.5f
                setTextColor(Color.rgb(20, 30, 45))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 8, 0, 2)
            })
            explainContainer.addView(TextView(this).apply {
                text = content
                textSize = 12.5f
                setTextColor(Color.rgb(60, 70, 85))
                setPadding(0, 0, 0, 6)
                setLineSpacing(3f, 1f)
            })
        }
        column.addView(explainContainer)

        column.note("Bộ đếm là chỉ báo logic thời gian thực của demo.\nKhi bảo vệ đề tài, kết hợp chỉ số này cùng Leak Trace và Profiler để minh chứng toàn diện.", 12f)

        column.action("🗑 Xóa kết quả đã lưu") {
            ResultStore.clear(this)
            recreate()
        }
        column.action("← Quay lại") { finish() }
    }

    private fun createLabel(text: String, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun createValue(text: String, color: Int) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(color)
        setTypeface(null, Typeface.BOLD)
    }
}
