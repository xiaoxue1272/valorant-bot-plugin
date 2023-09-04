package io.tiangou.delay

import io.ktor.util.date.*
import io.tiangou.AbstractTask
import kotlinx.coroutines.delay


abstract class DelayTask(
    delay: Long,
    delayWay: DelayWay,
) : AbstractTask() {

    val executeTime: GMTDate =  when(delayWay) {
        DelayWay.DELAY_UNTIL_TIMESTAMP -> GMTDate(delay + getTimeMillis())
        DelayWay.WAIT_SPECIFIC_TIMES -> GMTDate(delay)
    }

    enum class DelayWay {
        DELAY_UNTIL_TIMESTAMP,
        WAIT_SPECIFIC_TIMES,
    }


    override fun enable() {
        start {
            delay(executeTime.timestamp - getTimeMillis())
            run()
        }
    }

    override fun disable() {
        stop()
    }
}