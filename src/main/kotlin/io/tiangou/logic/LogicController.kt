package io.tiangou.logic

import io.tiangou.reply
import io.tiangou.repository.LogicRepository
import kotlinx.coroutines.*
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder

class LogicController {

    var processJob: Job? = null

    fun loadLogic(message: String): List<LogicProcessor<MessageEvent>>? = LogicRepository.find(message)

    suspend fun exitLogic(event: MessageEvent) {
        if (processJob?.isActive == true) {
            processJob!!.cancel("用户退出指令")
        } else event.reply("没有正在执行的指令或正在退出中,请勿重复执行")
    }

    fun logicTips(key: String) =
        LogicRepository.matchesKeys(key).takeIf { it.isNotEmpty() && key.isNotEmpty() }?.let {
            MessageChainBuilder()
                .append("未找到对应的操作,你可能想说:\n")
                .append(">>>>>>>>>\n")
                .append(it.joinToString("\n\n"))
                .build()
        }



}