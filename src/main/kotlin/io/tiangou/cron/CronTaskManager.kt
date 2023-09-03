@file:Suppress("unused")

package io.tiangou.cron

import cn.hutool.cron.pattern.CronPattern
import cn.hutool.cron.pattern.CronPatternUtil
import io.ktor.util.date.*
import io.tiangou.Global
import io.tiangou.TimeZoneSerializer
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.utils.MiraiLogger
import java.time.ZoneId
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

object CronTaskManager : AutoSavePluginConfig("cron-task") {

    @ValueName("taskList")
    @ValueDescription("定时任务集合")
    val taskList: MutableList<Task> by value(
        mutableListOf(
            DailyStorePushTask("0 10 08 * * ? *", true),
            PersistenceDataFlushTask("0 0 9 ? * 7 *", true),
            RiotAccountSecurityDataFlushTask("0 30 4,12,20 * * ? *", true),
            RiotAccountLoginExpiredTask("0 0 12 * * ? *", true)
        )
    )

    fun allTask(): List<Task> = taskList

    fun allTask(canManuallyTrigger: Boolean): List<Task> =
        taskList.filter { it.canManuallyTrigger == canManuallyTrigger }

    fun find(taskClass: KClass<Task>): Task? = find(taskClass.simpleName!!)

    fun find(taskName: String): Task? =
        taskList.find { it::class.simpleName!!.uppercase() == taskName.uppercase() }

    fun start() {
        taskList.forEach {
            it.takeIf { it.isEnable }?.enable()
        }
    }

    fun stop() {
        taskList.forEach {
            it.takeIf { it.isEnable }?.disable()
        }
    }

}

@Serializable
sealed class Task : CoroutineScope {

    @Transient
    protected val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    abstract var cron: String

    abstract var isEnable: Boolean

    abstract val description: String

    open val canManuallyTrigger: Boolean = true

    @Serializable(TimeZoneSerializer::class)
    open val timeZone: TimeZone = TimeZone.getTimeZone(ZoneId.of("GMT+08:00"))

    @Transient
    final override val coroutineContext: CoroutineContext =
        Global.coroutineScope.coroutineContext + CoroutineName("Task :${this::class::simpleName}")

    @Transient
    internal lateinit var cronPattern: CronPattern

    @Transient
    internal var job: Job? = null

    open fun enable() {
        cronPattern = CronPattern.of(cron)
        job = launch {
            while (true) {
                val calendar = Calendar.getInstance(timeZone)
                val nowTimeMillis = calendar.timeInMillis
                val waitOnExecuteTimeMillis =
                    CronPatternUtil.nextDateAfter(cronPattern, calendar.time, true).time - nowTimeMillis
                delay(waitOnExecuteTimeMillis)
                val startTimeMillis = getTimeMillis()
                executeRunCaching()
                val remainingTime = getTimeMillis() - startTimeMillis
                if (remainingTime < 1000) {
                    delay(1000 - remainingTime)
                }
            }
        }
        log.info("已启用任务 [${this::class.simpleName}]")
    }

    open fun disable() {
        runCatching { job?.cancel("已禁用任务 [${this::class.simpleName}] ") }
        log.info("已禁用任务 [${this::class.simpleName}]")
    }

    abstract suspend fun execute()

    suspend fun executeRunCaching() =
        runCatching {
            log.info("$description, 开始")
            execute()
            log.info("$description, 结束")
        }.onFailure {
            log.warning("$description, 执行时发生异常:", it)
        }


}



