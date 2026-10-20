package com.example.wifikeep

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            text = "Giữ Wi-Fi luôn hoạt động khi tắt màn hình"
        }

        val btnBattery = Button(this).apply {
            text = "1. Bỏ tối ưu hóa pin"
            setOnClickListener { requestIgnoreBattery() }
        }

        val btnStart = Button(this).apply {
            text = "2. Bật giữ Wi-Fi"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, WifiKeepService::class.java))
                status.text = "ĐANG BẬT: Wi-Fi được giữ hoạt động"
            }
        }

        val btnStop = Button(this).apply {
            text = "Tắt giữ Wi-Fi"
            setOnClickListener {
                stopService(Intent(this@MainActivity, WifiKeepService::class.java))
                status.text = "ĐÃ TẮT"
            }
        }

        layout.addView(status)
        layout.addView(btnBattery)
        layout.addView(btnStart)
        layout.addView(btnStop)
        setContentView(layout)
    }

    private fun requestIgnoreBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}
