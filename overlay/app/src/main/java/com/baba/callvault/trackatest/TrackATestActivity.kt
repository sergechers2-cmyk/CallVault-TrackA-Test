package com.baba.callvault.trackatest

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.baba.callvault.server.RecorderConnection
import com.baba.callvault.server.RecorderServerLauncher
import com.baba.callvault.services.recording.handoff.TrackAProbe

class TrackATestActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var connectButton: Button
    private lateinit var runButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "CallVault Track A DIAGNOSTIC 3"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "CallVault — Track A DIAGNOSTIC 3"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Это отдельное тестовое приложение с собственным package ID и TEST RecorderServer. Основной CallVault не обновляется и не заменяется. Сначала при необходимости настрой этот экземпляр CallVault, затем подключи его TEST daemon."
            textSize = 16f
            setPadding(0, dp(12), 0, dp(12))
        })

        statusView = TextView(this).apply {
            textSize = 15f
            text = currentStatus()
        }
        root.addView(statusView)

        val setupButton = Button(this).apply {
            text = "Открыть setup CallVault"
            setOnClickListener {
                startActivity(Intent(this@TrackATestActivity, com.baba.callvault.MainActivity::class.java))
            }
        }
        root.addView(setupButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        connectButton = Button(this).apply {
            text = "Подключить TEST daemon"
            setOnClickListener { connectTestDaemon() }
        }
        root.addView(connectButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        runButton = Button(this).apply {
            text = "Проверить Track A"
            isEnabled = false
            setOnClickListener { runProbe() }
        }
        root.addView(runButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        stopButton = Button(this).apply {
            text = "Остановить TEST daemon"
            setOnClickListener { stopTestDaemon() }
        }
        root.addView(stopButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        root.addView(TextView(this).apply {
            text = "Важно: не выполняй этот тест во время телефонного разговора. После теста можно остановить TEST daemon и снова открыть основной CallVault."
            textSize = 14f
            setPadding(0, dp(16), 0, 0)
        })

        root.addView(TextView(this).apply {
            text = "Результаты: POSITIVE — механизм запуска held VOICE_CALL track прошёл; START_REFUSED/NEGATIVE — данный путь запуска не проходит на этом OEM."
            textSize = 14f
            setPadding(0, dp(10), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun currentStatus(): String {
        val alive = runCatching {
            RecorderConnection.service?.asBinder()?.pingBinder() == true
        }.getOrDefault(false)
        return if (alive) {
            "Статус: TEST recorder daemon доступен. Можно запускать Track A."
        } else {
            "Статус: TEST recorder daemon не подключён. Открой setup и/или подключи TEST daemon."
        }
    }

    private fun refreshState() {
        val alive = runCatching {
            RecorderConnection.service?.asBinder()?.pingBinder() == true
        }.getOrDefault(false)
        statusView.text = currentStatus()
        runButton.isEnabled = alive
    }

    private fun connectTestDaemon() {
        connectButton.isEnabled = false
        runButton.isEnabled = false
        statusView.text = "Подключаем TEST daemon через ADB…"
        Thread {
            val ok = runCatching {
                RecorderServerLauncher.ensureServerRunning(this, 24_000)
            }.getOrElse { false }
            runOnUiThread {
                statusView.text = if (ok) {
                    "Статус: TEST daemon подключён."
                } else {
                    "TEST daemon не подключён. Проверь setup, pairing/Wireless debugging и повтори."
                }
                connectButton.isEnabled = true
                runButton.isEnabled = ok
            }
        }.apply {
            isDaemon = true
            name = "track-a-connect"
            start()
        }
    }

    private fun stopTestDaemon() {
        runButton.isEnabled = false
        stopButton.isEnabled = false
        statusView.text = "Останавливаем TEST daemon…"
        Thread {
            val ok = runCatching {
                RecorderConnection.service?.destroy()
                true
            }.getOrDefault(false)
            runOnUiThread {
                statusView.text = if (ok) {
                    "TEST daemon остановлен. Основной CallVault не изменён."
                } else {
                    "TEST daemon не удалось остановить."
                }
                stopButton.isEnabled = true
                refreshState()
            }
        }.apply {
            isDaemon = true
            name = "track-a-stop"
            start()
        }
    }

    private fun runProbe() {
        runButton.isEnabled = false
        statusView.text = "Выполняется Track A probe… ничего не записывается."
        Thread {
            val result = runCatching { TrackAProbe.run() }.getOrElse { throwable ->
                Bundle().apply {
                    putString("result", "EXCEPTION — ${throwable.javaClass.simpleName}: ${throwable.message}")
                }
            }
            val text = result.getString("result") ?: result.toString()
            runOnUiThread {
                statusView.text = "Результат:\n$text"
                runButton.isEnabled = RecorderConnection.service?.asBinder()?.pingBinder() == true
            }
        }.apply {
            isDaemon = true
            name = "track-a-probe"
            start()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
