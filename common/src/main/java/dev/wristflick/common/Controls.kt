package dev.wristflick.common

import kotlin.math.abs

enum class Command { PREVIOUS, NEXT, LIKE, STATUS }

/** Coordinates are fractions of TikTok's current main window, not fixed pixels. */
data class Stroke(val x: Float, val startY: Float, val endY: Float,
                  val startMs: Long, val durationMs: Long)

object Controls {
    /** Slow drags count too. Horizontal/diagonal and short movements do not. */
    fun watchSwipe(dx: Float, dy: Float, thresholdPx: Float): Command? {
        if (abs(dy) < thresholdPx || abs(dy) < abs(dx) * 1.4f) return null
        return if (dy < 0) Command.PREVIOUS else Command.NEXT
    }

    fun phoneStrokes(command: Command): List<Stroke> = when (command) {
        // The directions intentionally invert the requested watch gestures.
        Command.PREVIOUS -> listOf(Stroke(.48f, .28f, .73f, 0, 280))
        Command.NEXT -> listOf(Stroke(.48f, .73f, .28f, 0, 280))
        Command.LIKE -> listOf(
            Stroke(.42f, .45f, .45f, 0, 45),
            Stroke(.42f, .45f, .45f, 145, 45)
        )
        Command.STATUS -> emptyList()
    }
}
