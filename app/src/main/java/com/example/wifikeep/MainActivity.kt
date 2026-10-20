package com.example.wifikeep

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var logView: TextView
    private lateinit var statusView: TextView

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "log") refreshLog()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        statusView = TextView(this).apply {
            textSize = 15f
            text = "Làm lần lượt các bước 1 → 2 → 3 bên dưới."
        }

        val ssidInput = EditText(this).apply {
            hint = "Tên Wi-Fi (SSID)"
            setText(SuggestionHelper.ssid(this@MainActivity))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val passInput = EditText(this).apply {
            hint = "Mật khẩu Wi-Fi"
            setText(SuggestionHelper.pass(this@MainActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }

        val btnBattery = Button(this).apply {
            text = "1. Bỏ tối ưu hóa pin"
            setOnClickListener { requestIgnoreBattery() }
        }

        val btnRegister = Button(this).apply {
            text = "2. Lưu và đăng ký mạng Wi-Fi"
            setOnClickListener {
                SuggestionHelper.save(
                    this@MainActivity,
                    ssidInput.text.toString().trim(),
                    passInput.text.toString()
                )
                val result = SuggestionHelper.register(this@MainActivity)
                statusView.text = result
                Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
            }
        }

        val btnStart = Button(this).apply {
            text = "3. Bật giữ Wi-Fi và theo dõi"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, WifiKeepService::class.java))
                statusView.text = "ĐANG BẬT giữ và theo dõi Wi-Fi"
            }
        }

        val btnStop = Button(this).apply {
            text = "Tắt giữ Wi-Fi"
            setOnClickListener {
                stopService(Intent(this@MainActivity, WifiKeepService::class.java))
                statusView.text = "ĐÃ TẮT"
            }
        }

        val btnClear = Button(this).apply {
            text = "Xóa nhật ký"
            setOnClickListener { EventLog.clear(this@MainActivity); refreshLog() }
        }

        val logTitle = TextView(this).apply {
            text = "Nhật ký (mới nhất ở trên):"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(4))
        }

        logView = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }

        root.addView(statusView)
        root.addView(ssidInput)
        root.addView(passInput)
        root.addView(btnBattery)
        root.addView(btnRegister)
        root.addView(btnStart)
        root.addView(btnStop)
        root.addView(btnClear)
        root.addView(logTitle)
        root.addView(logView)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
        refreshLog()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun refreshLog() {
        val text = EventLog.get(this)
        logView.text = if (text.isBlank()) "(chưa có sự kiện nào)" else text
    }

    private fun requestIgnoreBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Đã bỏ tối ưu hóa pin rồi", Toast.LENGTH_SHORT).show()
        }
    }
}
