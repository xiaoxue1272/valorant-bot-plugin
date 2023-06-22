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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EventHandler : SimpleListenerHost() {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

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
        val userCache = UserCacheRepository[sender.id]
        userCache.logicSelector.apply {
            if (!isStatusNormal()) {
                clear()
            }
            if (processJob != null && !processJob!!.isCancelled) {
                reply("正在执行中,请稍候")
                return
            }
            processJob = Global.coroutineScope.launch {
                runCatching {
                    while (true) {
                        if (!loadLogic(this@onMessage.message.toText()).process(this@onMessage, userCache)) {
                            break
                        }
                    }
                }.onFailure {
                    if (it is ValorantRuntimeException) {
                        log.warn("qq user:[${sender.id}]", it)
                        reply("${it.message}")
                    } else {
                        log.warn("processing valo bot logic throw throwable", it)
                        reply("error: ${it.message}")
                    }
                    isRunningError = true
                }
            }
            processJob!!.invokeOnCompletion {
                processJob = null
            }
            processJob!!.join()
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

