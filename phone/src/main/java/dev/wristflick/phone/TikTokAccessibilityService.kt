package dev.wristflick.phone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import dev.wristflick.common.Command
import dev.wristflick.common.Controls

class TikTokAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: TikTokAccessibilityService? = null
            private set
        private val TIKTOK_PACKAGES = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    }

    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var registered = false
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) RemoteState.disarm("Phone locked. Enable a new session.")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        RemoteState.disarm("Accessibility ready. Enable a remote session.")
        if (!registered) {
            if (Build.VERSION.SDK_INT >= 33)
                registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)
            else registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
            registered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { RemoteState.disarm("Accessibility interrupted. Enable a new session.") }
    override fun onDestroy() {
        if (instance === this) instance = null
        RemoteState.disarm("Accessibility disconnected.")
        if (registered) unregisterReceiver(screenOff)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** Called on the main thread only. Completion acknowledges touch dispatch, not TikTok state. */
    fun execute(command: Command, done: (Boolean, String) -> Unit) {
        if (!RemoteState.isArmed()) { done(false, "Enable remote on phone"); return }
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!power.isInteractive || keyguard.isKeyguardLocked) {
            RemoteState.disarm("Phone locked. Enable a new session.")
            done(false, "Unlock phone and enable remote"); return
        }
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() !in TIKTOK_PACKAGES) {
            done(false, "Open TikTok's video feed"); return
        }
        val blockedWindow = windows.any {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD ||
                (it.isFocused && it.id != root.windowId)
        }
        if (blockedWindow) { done(false, "Close keyboard or other windows"); return }
        if (busy) { done(false, "Wait for the current gesture"); return }
        val bounds = Rect().also { root.getBoundsInScreen(it) }
        if (bounds.width() < 200 || bounds.height() < 300) {
            done(false, "Open TikTok full screen"); return
        }
        if (command == Command.STATUS) { done(true, "Ready · TikTok in front"); return }

        val builder = GestureDescription.Builder()
        Controls.phoneStrokes(command).forEach { stroke ->
            val x = bounds.left + bounds.width() * stroke.x
            val y1 = bounds.top + bounds.height() * stroke.startY
            val y2 = bounds.top + bounds.height() * stroke.endY
            val path = Path().apply {
                moveTo(x, y1)
                if (y1 != y2) lineTo(x, y2)
            }
            builder.addStroke(GestureDescription.StrokeDescription(path, stroke.startMs, stroke.durationMs))
        }

        busy = true
        var finished = false
        fun finish(ok: Boolean, message: String) {
            if (finished) return
            finished = true
            busy = false
            done(ok, message)
        }
        val watchdog = Runnable { finish(false, "Gesture result unknown; check phone") }
        handler.postDelayed(watchdog, 1500)
        try {
            val accepted = dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    handler.removeCallbacks(watchdog)
                    val message = when (command) {
                        Command.PREVIOUS -> "Previous swipe sent"
                        Command.NEXT -> "Next swipe sent"
                        Command.LIKE -> "Like double-tap sent"
                        Command.STATUS -> "Ready"
                    }
                    finish(true, message)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    handler.removeCallbacks(watchdog)
                    finish(false, "Gesture cancelled; check phone")
                }
            }, handler)
            if (!accepted) {
                handler.removeCallbacks(watchdog)
                finish(false, "Android rejected the gesture")
            }
        } catch (_: RuntimeException) {
            handler.removeCallbacks(watchdog)
            finish(false, "Could not perform gesture")
        }
    }
}
