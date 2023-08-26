package io.tiangou.repository

import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.logic.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import net.mamoe.mirai.event.events.MessageEvent

object LogicRepository : JsonStorage<Map<String, List<LogicProcessor<MessageEvent>>>>(
    "logic-list", StoragePathEnum.CONFIG_PATH, serializer()
) {

    private val logicConfig: Map<String, List<LogicProcessor<MessageEvent>>> =
        runBlocking { load() ?: store(DEFAULT_LOGIC_MAP) }

    internal fun find(key: String): List<LogicProcessor<MessageEvent>>? = logicConfig[key]

    internal fun matchesKeys(key: String): List<String> = logicConfig.keys.filter { it.contains(key) }

}

private val DEFAULT_LOGIC_MAP: Map<String, List<LogicProcessor<MessageEvent>>> by lazy {
    mapOf(
        "帮助" to listOf(HelpListLogicProcessor),
        "账号登录" to listOf(
            LoginRiotAccountLogicProcessor,
        ),
        "设置地区" to listOf(
            ChangeLocationShardLogicProcessor,
        ),
        "查询商店" to listOf(
            CheckRiotStatusAndSettingProcessor,
            QueryPlayerDailyStoreProcessor
        ),
        "更新每日商店推送任务状态" to listOf(
            CheckRiotStatusAndSettingProcessor,
            CheckIsBotFriendProcessor,
            SubscribeTaskDailyStoreProcessor
        ),
        "自定义商店背景图" to listOf(
            UploadCustomBackgroundProcessor
        ),
        "查询配件商店" to listOf(
            CheckRiotStatusAndSettingProcessor,
            QueryPlayerAccessoryStoreProcessor
        ),
        "添加群到每日商店推送地点" to listOf(
            CheckRiotStatusAndSettingProcessor,
            CheckIsBotFriendProcessor,
            AddLocateToDailyStorePushLocatesProcessor
        ),
        "设置当前地点为每日商店推送地点" to listOf(
            CheckRiotStatusAndSettingProcessor,
            CheckIsBotFriendProcessor,
            AddCurrentLocateToDailyStorePushLocatesProcessor
        )
    )
}

