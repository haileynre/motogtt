package dev.wristflick.phone

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var serviceStatus: TextView
    private lateinit var sessionStatus: TextView
    private lateinit var lastResult: TextView
    private lateinit var sessionButton: Button
    private val tick = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(14, 19, 27))
            addView(column)
        }
        // Handles Android 15+ edge-to-edge status/navigation bars without hiding buttons.
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        setContentView(scroll)
        scroll.requestApplyInsets()
        column.addView(label("WristFlick", 32f, Color.rgb(99, 230, 216)))
        column.addView(label("Your watch. Your next video.", 18f))
        column.addView(label("GALAXY WATCH 8 + ANDROID", 12f, Color.LTGRAY))
        column.addView(label("Swipe UP on watch  →  Previous video\n\nSwipe DOWN on watch  →  Next video\n\nDouble-tap watch  →  Like video", 19f))
        serviceStatus = label("", 16f)
        sessionStatus = label("", 16f)
        lastResult = label("", 15f, Color.LTGRAY)
        column.addView(serviceStatus)
        column.addView(button("1. Set up accessibility") { showDisclosure() })
        column.addView(sessionStatus)
        sessionButton = button("2. Enable remote for 30 minutes") {
            if (RemoteState.isArmed()) RemoteState.disarm()
            else if (TikTokAccessibilityService.instance == null) showDisclosure()
            else RemoteState.arm()
            refresh()
        }
        column.addView(sessionButton)
        column.addView(label("3. Open TikTok's full-screen video feed on this phone, then open WristFlick on your watch.", 17f))
        column.addView(lastResult)
        column.addView(label("The phone must stay unlocked with TikTok in front. Close comments, keyboards, menus and dialogs before using the remote. Locking the phone ends the session.", 15f, Color.LTGRAY))
        column.addView(label("No TikTok login is needed in WristFlick. It reads the active app's package and window bounds to target touches. It does not save screen contents, record video or use analytics. Commands travel through Google Play services between your paired devices.", 14f, Color.LTGRAY))
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

    private fun showDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Allow watch-controlled touches?")
            .setMessage("WristFlick uses Android Accessibility to check the foreground app and window bounds, and to swipe or double-tap TikTok when you gesture on your watch. Android grants powerful screen interaction access. WristFlick uses it only during a session you enable. It does not store screen contents.\n\nIn the next screen, find WristFlick under downloaded/installed apps and enable it.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.show()
    }

    private fun refresh() {
        val connected = TikTokAccessibilityService.instance != null
        serviceStatus.text = if (connected) "Accessibility: ready" else "Accessibility: not enabled"
        sessionStatus.text = if (RemoteState.isArmed())
            "Remote: enabled · ${(RemoteState.remainingMs() + 59_999) / 60_000} min left"
        else "Remote: disabled"
        sessionButton.text = if (RemoteState.isArmed()) "Disable remote" else "2. Enable remote for 30 minutes"
        lastResult.text = RemoteState.lastResult
    }

    private fun label(value: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        setPadding(0, dp(12), 0, dp(12))
    }
    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8); bottomMargin = dp(8)
        }
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
