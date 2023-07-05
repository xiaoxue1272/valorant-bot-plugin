package io.tiangou

import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import net.mamoe.mirai.utils.MiraiLogger

object EventHandler : SimpleListenerHost() {

    private val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    @EventHandler
    suspend fun BotInvitedJoinGroupRequestEvent.onMessage() {
        accept()
    }

    @EventHandler
    suspend fun NewFriendRequestEvent.onMessage() {
        accept()
    }

    @EventHandler
    suspend fun MessageEvent.onMessage() {
        if (!checkMessageAllow()) {
            return
        }
        UserCacheRepository[sender.id].apply {
            logicSelector.run {
                if (processJob != null && processJob!!.isActive) {
                    reply("正在执行中,请稍候")
                    return
                }
                processJob = launch {
                    runCatching {
                        while (true) {
                            val logicProcessor = loadLogic(this@onMessage.message.toText())
                                ?: inputNotFoundHandle().let { return@launch }
                            if (!logicProcessor.process(this@onMessage, this@apply)) break
                        }
                    }.onFailure {
                        isRunningError = true
                        if (it is ValorantRuntimeException) {
                            log.warning("qq user:[${sender.id}]", it)
                            reply("${it.message}")
                        } else {
                            log.warning("processing valorant bot logic throw throwable", it)
                            reply("error: ${it.message}")
                        }
                    }
                    createTimeoutCancelJob(this@onMessage, this@apply)
                    cleanWhenCompleted()
                }
            }
        }
    }

    private fun MessageEvent.checkMessageAllow(): Boolean {
        if (Bot.getInstanceOrNull(sender.id) != null) {
            return false
        }
        return when (this) {
            is GroupMessageEvent -> isGroupMessageAllow()
            else -> true
        }
    }

    private fun GroupMessageEvent.isGroupMessageAllow(): Boolean =
        when (Global.eventConfig.groupMessageHandleStrategy) {
            Global.GroupMessageHandleEnum.AT_AND_QUOTE_REPLY -> message.firstIsInstanceOrNull<At>()?.target == bot.id
                    || message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id

            Global.GroupMessageHandleEnum.AT -> message.firstIsInstanceOrNull<At>()?.target == bot.id
            Global.GroupMessageHandleEnum.QUOTE_REPLY -> message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id
            Global.GroupMessageHandleEnum.NONE -> false
            Global.GroupMessageHandleEnum.ALL -> true
        }

    private suspend fun MessageEvent.inputNotFoundHandle() {
        if (Global.eventConfig.isWarnOnInputNotFound) reply("未找到对应操作,请检查输入是否正确")
    }

}



