package dev.wristflick.phone

import android.os.SystemClock

/** Session state deliberately does not survive process death, reboot or screen lock. */
object RemoteState {
    private const val SESSION_MS = 30 * 60 * 1000L
    private var enabledUntil = 0L
    var lastResult = "No gestures yet."
        private set

    fun arm() {
        enabledUntil = SystemClock.elapsedRealtime() + SESSION_MS
        lastResult = "Remote enabled. Open TikTok's video feed."
    }
    fun disarm(reason: String = "Remote disabled.") {
        enabledUntil = 0L
        lastResult = reason
    }
    fun remainingMs(): Long = (enabledUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0)
    fun isArmed(): Boolean = remainingMs() > 0
    fun report(message: String) { lastResult = message }
}
