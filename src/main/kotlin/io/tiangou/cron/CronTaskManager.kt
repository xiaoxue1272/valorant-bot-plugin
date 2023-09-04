@file:Suppress("unused")

package io.tiangou.cron

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value
import kotlin.reflect.KClass

object CronTaskManager : AutoSavePluginConfig("cron-task") {

    @ValueName("cronTaskList")
    @ValueDescription("定时任务集合")
    private val cronTaskList: MutableList<CronTask> by value(
        mutableListOf(
            DailyStorePushCronTask("0 10 08 * * ? *", true),
            PersistenceDataFlushCronTask("0 0 9 ? * 7 *", true),
            RiotAccountSecurityDataFlushCronTask("0 30 4,12,20 * * ? *", true),
            RiotAccountLoginExpiredCronTask("0 0 12 * * ? *", true)
        )
    )

    fun allTask(): List<CronTask> = cronTaskList

    fun find(cronTaskClass: KClass<CronTask>): CronTask? = find(cronTaskClass.simpleName!!)

    fun find(taskName: String): CronTask? =
        cronTaskList.find { it::class.simpleName!!.uppercase() == taskName.uppercase() }

    fun start() {
        cronTaskList.forEach {
            it.takeIf { it.isEnable }?.enable()
        }
    }

    fun stop() {
        cronTaskList.forEach {
            it.takeIf { it.isEnable }?.disable()
        }
    }

}





