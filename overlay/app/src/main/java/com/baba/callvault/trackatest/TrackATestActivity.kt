package com.baba.callvault.trackatest

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.baba.callvault.server.RecorderConnection
import com.baba.callvault.services.recording.handoff.TrackAProbe

class TrackATestActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Track A Test"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        root.addView(TextView(this).apply {
            text = "CallVault — Track A"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Первый безопасный тест. Проверяем, может ли приложение самостоятельно запустить VOICE_CALL track, созданный privileged daemon. Звонок пока не нужен."
            textSize = 16f
            setPadding(0, dp(12), 0, dp(12))
        })
        statusView = TextView(this).apply {
            textSize = 15f
            text = currentStatus()
        }
        root.addView(statusView)
        runButton = Button(this).apply {
            text = "Проверить Track A"
            setOnClickListener { runProbe() }
        }
        root.addView(runButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        root.addView(TextView(this).apply {
            text = "Результаты: POSITIVE — хорошо; START_REFUSED/NEGATIVE — этот способ на данном OEM не проходит."
            textSize = 14f
            setPadding(0, dp(16), 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        statusView.text = currentStatus()
    }

    private fun currentStatus(): String {
        val alive = runCatching { RecorderConnection.service?.asBinder()?.pingBinder() == true }.getOrDefault(false)
        return if (alive) "Статус: recorder daemon доступен. Можно запускать тест."
        else "Статус: recorder daemon пока не подключён. Сначала должен быть выполнен обычный setup CallVault."
    }

    private fun runProbe() {
        runButton.isEnabled = false
        statusView.text = "Выполняется Track A probe… ничего не записывается."
        Thread {
            val result = runCatching { TrackAProbe.run() }.getOrElse { t ->
                Bundle().apply { putString("result", "EXCEPTION — ${t.javaClass.simpleName}: ${t.message}") }
            }
            val text = result.getString("result") ?: result.toString()
            runOnUiThread {
                statusView.text = "Результат:\n$text"
                runButton.isEnabled = true
            }
        }.apply {
            isDaemon = true
            name = "track-a-probe"
            start()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
