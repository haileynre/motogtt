package dev.wristflick.common

import org.junit.Assert.*
import org.junit.Test

class ControlsTest {
    @Test fun watchUpGoesBackButPhoneSwipesDown() {
        val command = Controls.watchSwipe(2f, -90f, 40f)
        assertEquals(Command.PREVIOUS, command)
        val stroke = Controls.phoneStrokes(command!!).single()
        assertTrue(stroke.endY > stroke.startY)
    }
    @Test fun watchDownGoesNextButPhoneSwipesUp() {
        val command = Controls.watchSwipe(0f, 90f, 40f)
        assertEquals(Command.NEXT, command)
        val stroke = Controls.phoneStrokes(command!!).single()
        assertTrue(stroke.endY < stroke.startY)
    }
    @Test fun shortAndHorizontalMotionsDoNothing() {
        assertNull(Controls.watchSwipe(0f, 39f, 40f))
        assertNull(Controls.watchSwipe(100f, 60f, 40f))
        assertNull(Controls.watchSwipe(100f, -60f, 40f))
    }
    @Test fun likeIsTwoSeparateTapsInTheSamePlace() {
        val strokes = Controls.phoneStrokes(Command.LIKE)
        assertEquals(2, strokes.size)
        assertEquals(strokes[0].x, strokes[1].x)
        assertEquals(strokes[0].startY, strokes[1].startY)
        assertTrue(strokes.all { it.startY == it.endY })
        assertTrue(strokes[1].startMs > strokes[0].durationMs)
        assertTrue(strokes[1].startMs - strokes[0].durationMs < 300)
    }
    @Test fun statusNeverHasTouchStrokes() {
        assertTrue(Controls.phoneStrokes(Command.STATUS).isEmpty())
    }
    @Test fun oldAndFutureRequestsAreRejected() {
        assertTrue(Protocol.isFresh(100_000L, 100_000L))
        assertFalse(Protocol.isFresh(89_999L, 100_000L))
        assertFalse(Protocol.isFresh(110_001L, 100_000L))
    }
}
