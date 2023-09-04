package io.tiangou.cron

import cn.hutool.cron.pattern.CronPattern
import cn.hutool.cron.pattern.CronPatternUtil
import io.ktor.util.date.*
import io.tiangou.AbstractTask
import io.tiangou.TimeZoneSerializer
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.ZoneId
import java.util.*

@Serializable
abstract class CronTask : AbstractTask() {

    abstract var cron: String

    abstract var isEnable: Boolean

    @Serializable(TimeZoneSerializer::class)
    open val timeZone: TimeZone = TimeZone.getTimeZone(ZoneId.of("GMT+08:00"))

    @Transient
    internal lateinit var cronPattern: CronPattern

    override fun enable() {
        start {
            cronPattern = CronPattern.of(cron)
            while (true) {
                val calendar = Calendar.getInstance(timeZone)
                val nowTimeMillis = calendar.timeInMillis
                val waitOnExecuteTimeMillis =
                    CronPatternUtil.nextDateAfter(cronPattern, calendar.time, true).time - nowTimeMillis
                delay(waitOnExecuteTimeMillis)
                val startTimeMillis = getTimeMillis()
                run()
                val remainingTime = getTimeMillis() - startTimeMillis
                if (remainingTime < 1000) {
                    delay(1000 - remainingTime)
                }
            }
        }
        log.info("Cron Task [${this::class.simpleName}] 已启用")
    }

    override fun disable() {
        stop()
        log.info("Cron Task [${this::class.simpleName}] 已禁用")
    }


}