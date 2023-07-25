package io.tiangou

import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import net.mamoe.mirai.utils.MiraiLogger

object EventHandler : SimpleListenerHost() {

    private val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    @EventHandler
    suspend fun BotInvitedJoinGroupRequestEvent.onMessage() {
        if (Global.eventConfig.autoAcceptGroupRequest) {
            accept()
        }
    }

    @EventHandler
    suspend fun NewFriendRequestEvent.onMessage() {
        if (Global.eventConfig.autoAcceptFriendRequest) {
            accept()
        }
    }

    @EventHandler
    suspend fun MessageEvent.onMessage() {
        if (!checkMessageAllow()) {
            return
        }
        UserCacheRepository[sender.id].apply {
            if (Global.eventConfig.exitLogicCommand == message.toText()) {
                exitLogic()
                return
            }
            logicSelector.run {
                if (processJob != null && processJob!!.isActive) {
                    reply("正在执行中,请稍候")
                    return
                }
                processJob = launch {
                    runCatching {
                        while (true) {
                            val logicProcessor = loadLogic(this@onMessage.message.toText())
                                ?: inputNotFoundHandle("未找到对应操作,请检查输入是否正确").let { return@launch }
                            if (!logicProcessor.synchronousProcess(this@onMessage, this@apply)) break
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

    private suspend fun MessageEvent.checkMessageAllow(): Boolean {
        if (Bot.getInstanceOrNull(sender.id) != null) {
            return false
        }
        return isAllow()
    }

    private suspend fun MessageEvent.isAllow(): Boolean {
        if (this is GroupMessageEvent) {
            if (!isGroupMessageAllow()) {
                return false
            }
        }
        when (VisitConfig.controlType) {
            VisitControlEnum.WHITE_LIST -> if (!subject.getVisitControlList().contains(subject.id)) inputNotFoundHandle("暂无操作权限").apply { return false }
            VisitControlEnum.BLACK_LIST -> if (subject.getVisitControlList().contains(subject.id)) inputNotFoundHandle("暂无操作权限").apply { return false }
        }
        return true
    }

    private fun GroupMessageEvent.isGroupMessageAllow(): Boolean =
        when (Global.eventConfig.groupMessageHandleStrategy) {
            GroupMessageHandleEnum.AT_AND_QUOTE_REPLY -> message.firstIsInstanceOrNull<At>()?.target == bot.id
                    || message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id

            GroupMessageHandleEnum.AT -> message.firstIsInstanceOrNull<At>()?.target == bot.id
            GroupMessageHandleEnum.QUOTE_REPLY -> message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id
            GroupMessageHandleEnum.NONE -> false
            GroupMessageHandleEnum.ALL -> true
        }

    private suspend fun MessageEvent.inputNotFoundHandle(msg: String) {
        if (Global.eventConfig.isWarnOnInputNotFound) reply(msg)
    }

}



