package io.tiangou

import io.tiangou.EventHandleConfig.GroupMessageHandleEnum.*
import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
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
                            log.warning("processing valo bot logic throw throwable", it)
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
        when (EventHandleConfig.config.groupMessageHandleStorage) {
            AT_AND_QUOTE_REPLY -> message.firstIsInstanceOrNull<At>()?.target == bot.id
                    || message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id

            AT -> message.firstIsInstanceOrNull<At>()?.target == bot.id
            QUOTE_REPLY -> message.firstIsInstanceOrNull<QuoteReply>()?.source?.fromId == bot.id
            NONE -> false
            ALL -> true
        }

    private suspend fun MessageEvent.inputNotFoundHandle() {
        if (EventHandleConfig.config.isWarnOnInputNotFound) reply("未找到对应操作,请检查输入是否正确")
    }

}

object EventHandleConfig : ReadOnlyPluginConfig("event-handle-config") {

    @ValueDescription("事件监听配置")
    val config: EventHandleConfigData by value(EventHandleConfigData())

    @Serializable
    data class EventHandleConfigData(
        @ValueDescription("当输入不正确时发出警告(为true时回复未知操作,false不回复消息)")
        val isWarnOnInputNotFound: Boolean = true,
        @ValueDescription(
            """
            配置群内消息监听策略: AT: 监听AT消息, AT_AND_QUOTE_REPLY: 监听AT和引用回复消息, QUOTE_REPLY: 监听引用回复消息, NONE: 不监听群内消息, ALL: 监听所有消息
            默认为: AT_AND_QUOTE_REPLY, 就是监听 AT消息和引用回复消息.
        """
        )
        val groupMessageHandleStorage: GroupMessageHandleEnum = AT_AND_QUOTE_REPLY,
        @ValueDescription("指令等待输入超时时间,请不要设置太短或太长,分为单位,默认5分钟")
        val waitTimeoutMinutes: Int = 5
    )

    @Serializable
    enum class GroupMessageHandleEnum {
        AT,
        QUOTE_REPLY,
        AT_AND_QUOTE_REPLY,
        NONE,
        ALL
    }

}



