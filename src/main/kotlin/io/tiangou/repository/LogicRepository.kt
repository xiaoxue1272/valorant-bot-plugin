package io.tiangou.repository

import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.logic.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import net.mamoe.mirai.event.events.MessageEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object LogicRepository : JsonStorage<Map<String, List<LogicProcessor<MessageEvent>>>>(
    "logic-list",
    StoragePathEnum.CONFIG_PATH,
    serializer()
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private val logicConfig: Map<String, List<LogicProcessor<MessageEvent>>> =
        runBlocking { load() ?: store(DEFAULT_LOGIC_MAP) }

    init {
        log.info("Valorant Bot Logic List loaded")
    }

    internal fun find(key: String): List<LogicProcessor<MessageEvent>> =
        logicConfig[key] ?: listOf(HelpListLogicProcessor)

}

val DEFAULT_LOGIC_MAP: Map<String, List<LogicProcessor<MessageEvent>>> = mapOf(
    "帮助" to listOf(HelpListLogicProcessor),
    "账号登录" to listOf(
        AskRiotUsernameLogicProcessor,
        SaveRiotUsernameLogicProcessor,
        AskRiotPasswordLogicProcessor,
        SaveRiotPasswordLogicProcessor,
        LoginRiotAccountLogicProcessor,
        VerifyRiotAccountLogicProcessor
    ),
    "设置地区" to listOf(
        AskLocationAreaLogicProcessor,
        SaveLocationShardLogicProcessor,
    ),
    "查询商店" to listOf(
        CheckRiotStatusAndSettingProcessor,
        QueryPlayerDailyStoreItemProcessor
    ),
    "更新每日商店推送任务状态" to listOf(
        SubscribeTaskDailyStore
    )
)

