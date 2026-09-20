package com.baba.callvault.trackatest

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.baba.callvault.MainActivity
import com.baba.callvault.server.RecorderConnection
import com.baba.callvault.services.recording.handoff.TrackAProbe

class TrackATestActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var runButton: Button
    private lateinit var armButton: Button
    private lateinit var measureButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Track A DIAGNOSTIC 4"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "CallVault — Track A DIAGNOSTIC 4"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Этот вариант работает внутри того же приложения CallVault и использует уже подключённый production RecorderServer. Он НЕ запускает новый daemon и НЕ пытается включать Wireless debugging."
            textSize = 16f
            setPadding(0, dp(12), 0, dp(12))
        })

        statusView = TextView(this).apply { textSize = 15f }
        root.addView(statusView)

        val mainButton = Button(this).apply {
            text = "Открыть основной CallVault"
            setOnClickListener {
                startActivity(Intent(this@TrackATestActivity, MainActivity::class.java))
            }
        }
        root.addView(mainButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        runButton = Button(this).apply {
            text = "1. Проверить Track A"
            setOnClickListener { runProbe() }
        }
        root.addView(runButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        armButton = Button(this).apply {
            text = "2. Arm: создать held track"
            setOnClickListener { armProbe() }
        }
        root.addView(armButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        measureButton = Button(this).apply {
            text = "3. Measure во время звонка"
            setOnClickListener { measureProbe() }
        }
        root.addView(measureButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        root.addView(TextView(this).apply {
            text = "Важно: кнопки 2–3 относятся к расширенному Track A тесту. Ничего не записывается в файл. Для Measure нужен активный телефонный звонок."
            textSize = 14f
            setPadding(0, dp(16), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val alive = runCatching {
            RecorderConnection.service?.asBinder()?.pingBinder() == true
        }.getOrDefault(false)
        statusView.text = if (alive) {
            "Статус production daemon: ПОДКЛЮЧЁН. Можно проверять Track A."
        } else {
            "Статус production daemon: НЕ ПОДКЛЮЧЁН. Никаких попыток запуска ADB этот экран не делает."
        }
        runButton.isEnabled = alive
        armButton.isEnabled = alive
        measureButton.isEnabled = alive
    }

    private fun runProbe() {
        setBusy("Проверяем Track A через уже подключённый daemon…")
        Thread {
            val result = runCatching { TrackAProbe.run() }.getOrElse { t ->
                Bundle().apply {
                    putString("result", "EXCEPTION — ${t.javaClass.simpleName}: ${t.message}")
                }
            }
            val text = result.getString("result") ?: result.toString()
            runOnUiThread {
                statusView.text = "Результат:\n$text"
                refreshState()
            }
        }.apply {
            isDaemon = true
            name = "track-a-probe"
            start()
        }
    }

    private fun armProbe() {
        setBusy("Arm: создаём held track через production daemon…")
        Thread {
            val result = runCatching { TrackAProbe.arm() }.getOrElse { t ->
                Bundle().apply {
                    putString("result", "EXCEPTION — ${t.javaClass.simpleName}: ${t.message}")
                }
            }
            val text = result.getString("result") ?: result.toString()
            runOnUiThread {
                statusView.text = "Arm:\n$text"
                refreshState()
            }
        }.apply {
            isDaemon = true
            name = "track-a-arm"
            start()
        }
    }

    private fun measureProbe() {
        setBusy("Measure: ожидаем/читаем VOICE_CALL во время активного звонка…")
        Thread {
            val result = runCatching { TrackAProbe.measure(this, 10) }.getOrElse { t ->
                Bundle().apply {
                    putString("result", "EXCEPTION — ${t.javaClass.simpleName}: ${t.message}")
                }
            }
            val text = result.getString("result") ?: result.toString()
            runOnUiThread {
                statusView.text = "Measure:\n$text"
                refreshState()
            }
        }.apply {
            isDaemon = true
            name = "track-a-measure"
            start()
        }
    }

    private fun setBusy(text: String) {
        statusView.text = text
        runButton.isEnabled = false
        armButton.isEnabled = false
        measureButton.isEnabled = false
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
