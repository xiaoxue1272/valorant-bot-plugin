package io.tiangou.repository

import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.logic.LogicProcessor
import io.tiangou.logic.default_logic_list
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import net.mamoe.mirai.event.events.MessageEvent

object LogicRepository : JsonStorage<Map<String, List<LogicProcessor<MessageEvent>>>>(
    "logic-list", StoragePathEnum.CONFIG_PATH, serializer()
) {

    private val logicConfig: Map<String, List<LogicProcessor<MessageEvent>>> =
        runBlocking { load() ?: store(default_logic_list.toMap()) }

    internal fun find(key: String): List<LogicProcessor<MessageEvent>>? = logicConfig[key]

    internal fun matchesKeys(key: String): List<String> = logicConfig.keys.filter { it.contains(key) }

}

