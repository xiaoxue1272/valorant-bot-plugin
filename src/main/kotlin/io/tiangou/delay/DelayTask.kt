package io.tiangou.delay

import io.tiangou.AbstractTask
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.time.Duration

abstract class DelayTask(
    private val delay: Duration,
) : AbstractTask(), Comparable<Duration> {

    final override fun compareTo(other: Duration): Int = delay.compareTo(other)

    override fun enable() {
        start {
            delay(delay)
            run()
            remove(this)
        }
        add(this)
    }

    override fun disable() {
        stop()
        remove(this)
    }

    protected companion object DelayTaskRegister : SortedSet<DelayTask> by ConcurrentSkipListSet()

}