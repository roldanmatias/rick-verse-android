package com.roldanmatias.rickverse.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.roldanmatias.rickverse"
private const val WAIT_MILLIS = 10_000L
private const val FLING_MARGIN = 0.1f

/** The list is ready when its filter chips are on screen ("All" chip). Semantic, not coordinates. */
internal fun MacrobenchmarkScope.waitForCastList() {
    device.wait(Until.hasObject(By.text("All")), WAIT_MILLIS)
}

internal fun MacrobenchmarkScope.scrollCastList() {
    // Scroll the root: the cast list is the only scrollable on the first screen.
    val list = device.findObject(By.scrollable(true)) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    repeat(3) { list.fling(Direction.DOWN) }
    device.waitForIdle()
    repeat(2) { list.fling(Direction.UP) }
    list.setGestureMargin((device.displayWidth * FLING_MARGIN).toInt())
}
