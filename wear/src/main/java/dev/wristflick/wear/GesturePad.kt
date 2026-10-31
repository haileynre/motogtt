package dev.wristflick.wear

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import dev.wristflick.common.Command
import dev.wristflick.common.Controls
import kotlin.math.min

class GesturePad(context: Context, private val command: (Command) -> Unit) : View(context) {
    var status = "Connecting…"
        set(value) { field = value; invalidate() }
    var statusOk = false
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var startX = 0f
    private var startY = 0f
    private var doubleConsumed = false
    private var multipleFingers = false
    private val threshold = maxOf(38f * resources.displayMetrics.density,
        ViewConfiguration.get(context).scaledTouchSlop * 3f)
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onDoubleTap(e: MotionEvent): Boolean {
            doubleConsumed = true
            if (!multipleFingers) performClick()
            return true
        }
    })

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "TikTok remote. Swipe up for previous video, down for next, double-tap to like."
    }

    override fun performClick(): Boolean {
        super.performClick()
        command(Command.LIKE)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            startX = event.x; startY = event.y
            doubleConsumed = false; multipleFingers = false
        }
        if (event.pointerCount > 1) multipleFingers = true
        if (multipleFingers) {
            val cancel = MotionEvent.obtain(event)
            cancel.action = MotionEvent.ACTION_CANCEL
            detector.onTouchEvent(cancel)
            cancel.recycle()
            return true
        }
        detector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> if (!doubleConsumed) {
                Controls.watchSwipe(event.x - startX, event.y - startY, threshold)?.let(command)
            }
            MotionEvent.ACTION_CANCEL -> doubleConsumed = true
        }
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean = when (action) {
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> { command(Command.PREVIOUS); true }
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> { command(Command.NEXT); true }
        else -> super.performAccessibilityAction(action, arguments)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(11, 16, 23))
        val size = min(width, height).toFloat()
        val x = width / 2f
        val y = height / 2f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * .004f
        paint.color = Color.rgb(35, 58, 69)
        canvas.drawCircle(x, y, size * .465f, paint)
        paint.style = Paint.Style.FILL
        text(canvas, "WRISTFLICK", x, y - size * .325f, size * .043f, Color.rgb(99, 230, 216), true)
        text(canvas, "↑  PREVIOUS", x, y - size * .18f, size * .057f, Color.WHITE)
        text(canvas, "♥", x, y + size * .005f, size * .15f, Color.rgb(255, 104, 133))
        text(canvas, "DOUBLE-TAP TO LIKE", x, y + size * .09f, size * .039f, Color.LTGRAY)
        text(canvas, "↓  NEXT", x, y + size * .235f, size * .057f, Color.WHITE)
        val color = if (statusOk) Color.rgb(99, 230, 216) else Color.rgb(255, 203, 130)
        // Two lines keep feedback inside the round 40 mm display's lower safe region.
        val words = status.split(' ')
        val lines = mutableListOf<String>()
        var line = ""
        for (word in words) {
            if ((line.length + word.length + 1) > 27 && line.isNotEmpty()) {
                lines.add(line); line = word
            } else line = if (line.isEmpty()) word else "$line $word"
        }
        if (line.isNotEmpty()) lines.add(line)
        lines.take(2).forEachIndexed { index, value ->
            text(canvas, value, x, y + size * (.335f + index * .047f), size * .035f, color)
        }
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float,
                     size: Float, color: Int, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.color = color
        canvas.drawText(value, x, y, paint)
    }
}
