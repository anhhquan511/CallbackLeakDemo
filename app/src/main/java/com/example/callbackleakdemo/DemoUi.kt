package com.example.callbackleakdemo

import android.app.Activity
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

fun Activity.demoColumn(): LinearLayout {
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 32, 24, 32)
        setBackgroundColor(Color.rgb(245, 247, 250))
    }
    setContentView(ScrollView(this).apply { addView(column) })
    return column
}

fun LinearLayout.note(value: String, size: Float = 16f): TextView =
    TextView(context).apply {
        text = value
        textSize = size
        setTextColor(Color.rgb(20, 30, 45))
        setPadding(0, 12, 0, 12)
        addView(this)
    }

fun LinearLayout.action(label: String, block: () -> Unit) {
    addView(Button(context).apply {
        text = label
        isAllCaps = false
        setOnClickListener { block() }
    })
}
