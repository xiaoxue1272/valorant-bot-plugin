@file:Suppress("unused")

package io.tiangou

import io.tiangou.config.PluginConfig
import io.tiangou.config.PluginConfig.EventConfigData.GroupMessageHandleEnum
import io.tiangou.config.VisitConfig
import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import net.mamoe.mirai.utils.MiraiLogger
import kotlin.coroutines.CoroutineContext

object EventHandler : SimpleListenerHost() {

    private val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        log.warning("io.tiangou.EventHandler exception catcher: catch an exception", exception)
    }

    @EventHandler
    suspend fun BotInvitedJoinGroupRequestEvent.onMessage() {
        if (PluginConfig.eventConfig.autoAcceptGroupRequest) {
            accept()
        }
    }

    @EventHandler
    suspend fun NewFriendRequestEvent.onMessage() {
        if (PluginConfig.eventConfig.autoAcceptFriendRequest) {
            accept()
        }
    }

    @EventHandler
    suspend fun MessageEvent.onMessage() {
        if (!isMessageAllow()) {
            return
        }
        val userCache = UserCacheRepository[sender.id]
        userCache.logicController.apply {
            if (PluginConfig.eventConfig.exitLogicCommand == toText()) {
                exitLogic(this@onMessage)
                return
            }
            if (processJob != null && processJob!!.isActive) {
                reply("正在执行中,请稍候")
                return
            }
            val key = this@onMessage.message.toText()
            val processorList = loadLogic(key)
            if (processorList == null) {
                logicTips(key)?.let { inputNotFoundHandle(it) }
                return
            }
            supervisorScope {
                processJob = launch {
                    runCatching {
                        for (logicProcessor in processorList) {
                            logicProcessor.apply { process(userCache) }
                        }
                    }.onFailure {
                        when (it) {
                            is ValorantPluginException -> {
                                log.warning("qq user:[${sender.id}]", it)
                                reply("${it.message}")
                            }

                            is CancellationException -> {}
                            else -> {
                                log.warning("processing valorant bot logic throw throwable", it)
                                reply("error: ${it.message}")
                            }
                        }
                    }
                }.apply {
                    invokeOnCompletion {
                        processJob = null
                        if (it != null && it is CancellationException) runBlocking { reply("已退出指令") }
                    }
                }
            }
        }
    }

    private suspend fun MessageEvent.isMessageAllow(): Boolean {
        if (Bot.getInstanceOrNull(sender.id) != null) {
            return false
        }
        if (this is GroupMessageEvent) {
            if (!isGroupMessageAllow()) {
                return false
            }
        }
        when (VisitConfig.controlType) {
            VisitConfig.VisitControlEnum.WHITE_LIST -> if (!subject.getVisitControlList()
                    .contains(subject.id)
            ) inputNotFoundHandle(
                "暂无操作权限"
            ).apply { return false }

            VisitConfig.VisitControlEnum.BLACK_LIST -> if (subject.getVisitControlList()
                    .contains(subject.id)
            ) inputNotFoundHandle("暂无操作权限").apply { return false }
        }
        return true
    }

    private fun GroupMessageEvent.isGroupMessageAllow(): Boolean =
        when (PluginConfig.eventConfig.groupMessageHandleStrategy) {
            GroupMessageHandleEnum.AT_AND_QUOTE_REPLY -> message.firstIsInstanceOrNull<At>()?.target == bot.id
                    || message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id

            GroupMessageHandleEnum.AT -> message.firstIsInstanceOrNull<At>()?.target == bot.id
            GroupMessageHandleEnum.QUOTE_REPLY -> message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id
            GroupMessageHandleEnum.NONE -> false
            GroupMessageHandleEnum.ALL -> true
        }

    private suspend infix fun MessageEvent.inputNotFoundHandle(message: String) {
        if (PluginConfig.eventConfig.isWarnOnInputNotFound) reply(message)
    }

    private suspend infix fun MessageEvent.inputNotFoundHandle(message: Message) {
        if (PluginConfig.eventConfig.isWarnOnInputNotFound) reply(message)
    }

}



