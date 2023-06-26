package io.tiangou.cron

import cn.hutool.cron.pattern.CronPattern
import cn.hutool.cron.pattern.CronPatternUtil
import io.ktor.util.date.*
import io.tiangou.Global
import io.tiangou.logic.utils.DailyStoreImageGenerator
import io.tiangou.other.http.actions
import io.tiangou.repository.UserCacheRepository
import io.tiangou.repository.ValorantThirdPartyPersistenceDataInitiator
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.message.data.Image.Key.isUploaded
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.MiraiLogger
import java.util.*
import kotlin.coroutines.CoroutineContext

object CronTaskManager : AutoSavePluginConfig("cron-task") {

    @ValueName("taskList")
    @ValueDescription("定时任务集合")
    val taskList: List<AbstractTask> by value(
        listOf(
            SubscribeDailyStore("0 10 08 * * ? *", true),
            ValorantPersistenceDataFlush("0 0 9 ? * 7 *", true)
        )
    )

    fun start() {
        taskList.forEach {
            it.takeIf { it.enable }?.onEnable()
        }
    }

    fun stop() {
        taskList.forEach {
            it.takeIf { it.enable }?.onDisable()
        }
    }

    fun <T : AbstractTask> enableTask(task: T) {
        task.onEnable()
    }

    fun <T : AbstractTask> disableTask(task: T) {
        task.onDisable()
    }

}

@Serializable
sealed class AbstractTask : CoroutineScope {

    @Transient
    val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    abstract var cron: String

    abstract var enable: Boolean

    @Transient
    final override val coroutineContext: CoroutineContext =
        Global.coroutineScope.coroutineContext + CoroutineName("Task :${this::class::simpleName}")

    @Transient
    private lateinit var cronPattern: CronPattern

    @Transient
    private lateinit var job: Job

    init {
        coroutineContext[Job]?.invokeOnCompletion { if (it != null) onDisable() }
    }

    fun onEnable() {
        cronPattern = CronPattern.of(cron)
        job = launch {
            while (true) {
                val waitOnExecuteTimeMillis =
                    CronPatternUtil.nextDateAfter(cronPattern, Date(), true).time - getTimeMillis()
                delay(waitOnExecuteTimeMillis)
                runCatching { execute() }
            }
        }
        log.info("已启用任务 [${this::class.simpleName}]")
    }

    fun onDisable() {
        runCatching { job.cancel("已禁用任务 [${this::class.simpleName}] ") }
        log.info("已禁用任务 [${this::class.simpleName}]")
    }

    abstract suspend fun execute()

}

@Serializable
class SubscribeDailyStore(
    override var cron: String,
    override var enable: Boolean
) : AbstractTask() {

    override suspend fun execute() {
        log.info("每日商店定时推送任务,开始")
        Bot.instances.forEach { bot ->
            UserCacheRepository.getAllUserCache().forEach { entry ->
                if (entry.value.subscribeDailyStore) {
                    bot.let {
                        it.getFriend(entry.key) ?: it.getStranger(entry.key)
                    }?.runCatching {
                        val dailyStore = entry.value.riotClientData.actions {
                            DailyStoreImageGenerator.generate(this)
                        }
                        var uploadImage = uploadImage(dailyStore.toExternalResource().toAutoCloseable())
                        repeat(2) {
                            if (!uploadImage.isUploaded(bot)) {
                                uploadImage = uploadImage(dailyStore.toExternalResource().toAutoCloseable())
                            }
                        }
                        sendMessage(uploadImage)
                    }?.onFailure {
                        log.warning("QQ:[${entry.key}],推送每日商店内容异常,异常信息:${it.message}")
                    }
                }
            }
        }
        log.info("每日商店定时推送任务,结束")
    }

}

@Serializable
class ValorantPersistenceDataFlush(
    override var cron: String,
    override var enable: Boolean
) : AbstractTask() {

    override suspend fun execute() {
        log.info("Valorant皮肤库数据刷新任务,开始")
        runCatching {
            ValorantThirdPartyPersistenceDataInitiator.init()
        }.onFailure {
            log.warning("")
        }
        log.info("Valorant皮肤库数据刷新任务,结束")
    }

}


