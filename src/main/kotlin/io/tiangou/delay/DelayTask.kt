package io.tiangou.delay

import io.tiangou.AbstractTask
import kotlinx.coroutines.delay
import kotlin.time.Duration

abstract class DelayTask(
    private val delay: Duration,
) : AbstractTask() {

    override fun enable() {
        start {
            delay(delay)
            run()
        }
    }

    override fun disable() {
        stop()
    }
}