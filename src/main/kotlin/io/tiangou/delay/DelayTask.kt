package io.tiangou.delay

import io.tiangou.AbstractTask
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.time.Duration

abstract class DelayTask(
    private val delay: Duration,
) : AbstractTask(), Comparable<DelayTask> {

    final override fun compareTo(other: DelayTask): Int = delay.compareTo(other.delay)

    override fun enable() {
        start {
            delay(delay)
            run()
            remove(this)
        }
        add(this)
        log.info("已启用延时任务:$description")
    }

    override fun disable() {
        stop()
        remove(this)
        log.info("已禁用延时任务:$description")
    }

    protected companion object DelayTaskRegister : SortedSet<DelayTask> by ConcurrentSkipListSet()

}