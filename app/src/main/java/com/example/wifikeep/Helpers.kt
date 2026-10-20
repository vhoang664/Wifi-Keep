package com.example.wifikeep

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PREFS_NAME = "wifikeep"

object EventLog {
    private const val KEY = "log"
    private const val MAX = 80

    @Synchronized
    fun add(ctx: Context, msg: String) {
        val p = ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
        val lines = (p.getString(KEY, "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
        lines.add("$time  $msg")
        p.edit().putString(KEY, lines.takeLast(MAX).joinToString("\n")).apply()
    }

    @Synchronized
    fun get(ctx: Context): String {
        val p = ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lines = (p.getString(KEY, "") ?: "").split("\n").filter { it.isNotBlank() }
        return lines.reversed().joinToString("\n")
    }

    @Synchronized
    fun clear(ctx: Context) {
        ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY, "").apply()
    }
}

object SuggestionHelper {

    fun save(ctx: Context, ssid: String, pass: String) {
        ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("ssid", ssid).putString("pass", pass).apply()
    }

    fun ssid(ctx: Context): String =
        ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("ssid", "NaCl") ?: "NaCl"

    fun pass(ctx: Context): String =
        ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("pass", "") ?: ""

    /** Đăng ký (hoặc đăng ký lại) mạng với hệ thống. Trả về câu mô tả kết quả. */
    fun register(ctx: Context): String {
        val app = ctx.applicationContext
        if (Build.VERSION.SDK_INT < 29) {
            return "Máy dùng Android dưới 10, không hỗ trợ tính năng này"
        }
        val ssid = ssid(app)
        val pass = pass(app)
        if (ssid.isBlank() || pass.length < 8) {
            return "Cần nhập tên Wi-Fi và mật khẩu (từ 8 ký tự)"
        }
        val result = try {
            val wm = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wm.removeNetworkSuggestions(emptyList())
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pass)
                .setIsAppInteractionRequired(false)
                .build()
            when (wm.addNetworkSuggestions(listOf(suggestion))) {
                0 -> "Thành công. Nếu có thông báo xin phép, hãy bấm Cho phép (Allow)"
                1 -> "Lỗi nội bộ của hệ thống"
                2 -> "Bị từ chối: hãy cho phép app trong thông báo hoặc cài đặt"
                3 -> "Mạng này đã được đăng ký trước đó"
                4 -> "Vượt quá số mạng cho phép"
                6 -> "Hệ thống đang không cho thêm mạng (Wi-Fi tắt?)"
                7 -> "Tên hoặc mật khẩu không hợp lệ"
                else -> "Mã kết quả không rõ"
            }
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
        EventLog.add(app, "Đăng ký mạng \"$ssid\": $result")
        return result
    }
}
