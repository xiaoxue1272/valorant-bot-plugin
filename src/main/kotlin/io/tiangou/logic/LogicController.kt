package io.tiangou.logic

import io.tiangou.reply
import io.tiangou.repository.LogicRepository
import kotlinx.coroutines.*
import net.mamoe.mirai.event.events.MessageEvent

class LogicController {

    var processJob: Job? = null

    fun loadLogic(message: String): List<LogicProcessor<MessageEvent>>? = LogicRepository.find(message)

    suspend fun MessageEvent.exitLogic() {
        if (processJob?.isActive == true) {
            processJob!!.cancel("用户退出指令")
        } else reply("没有正在执行的指令或正在退出中,请勿重复执行")
    }


}