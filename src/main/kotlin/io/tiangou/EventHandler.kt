package io.tiangou

import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.*
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
                if (processJob != null && !processJob!!.isCancelled) {
                    reply("正在执行中,请稍候")
                    return
                }
                processJob = launch {
                    runCatching {
                        while (true) {
                            val logicProcessor = loadLogic(this@onMessage.message.toText())
                            if (logicProcessor == null) {
                                reply("未找到对应操作,请检查输入是否正确")
                                break
                            }
                            if (!logicProcessor.process(this@onMessage, this@apply)) break
                        }
                    }.onFailure {
                        isRunningError = true
                        if (it is ValorantRuntimeException) {
                            log.warning("qq user:[${sender.id}]", it)
                            reply("${it.message}")
                        } else {
                            log.warning("processing valo bot logic throw throwable", it)
                            reply("error: ${it.message}")
                        }
                    }
                    autoClean()
                }
            }
        }
    }

    private fun MessageEvent.checkMessageAllow(): Boolean {
        if (sender.id == bot.id) {
            return false
        }
        return when (this) {
            is GroupMessageEvent -> message.firstIsInstanceOrNull<At>()?.target == bot.id
                    || message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id

            else -> true
        }
    }

    suspend fun MessageEvent.reply(message: String) {
        when (this) {
            is GroupMessageEvent -> group.sendMessage(MessageChainBuilder().append(At(sender)).append(message).build())
            else -> sender.sendMessage(message)
        }
    }

    suspend fun MessageEvent.reply(message: Message) {
        when (this) {
            is GroupMessageEvent -> group.sendMessage(MessageChainBuilder().append(At(sender)).append(message).build())
            else -> sender.sendMessage(message)
        }
    }

    fun MessageEvent.getContact(): Contact {
        return when (this) {
            is GroupMessageEvent -> group
            else -> sender
        }
    }
}

