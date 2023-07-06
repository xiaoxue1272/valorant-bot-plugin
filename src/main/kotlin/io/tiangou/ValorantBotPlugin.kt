package io.tiangou

import io.tiangou.cron.CronTaskCommand
import io.tiangou.cron.CronTaskManager
import io.tiangou.cron.StoreCachesCleanTask
import io.tiangou.repository.persistnce.PersistenceDataInitiator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.message.data.*
import java.io.File


fun MessageChain.toText() =
    MessageChainBuilder().apply { addAll(this@toText.filterIsInstance<PlainText>()) }.asMessageChain().content.trim()

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

suspend fun CommandSender.reply(message: String) {
    if (subject != null && user != null) {
        when (subject) {
            is Group -> sendMessage(MessageChainBuilder().append(At(user!!)).append(message).build())
            else -> sendMessage(message)
        }
    }
}

suspend fun CommandSender.reply(message: Message) {
    if (subject != null && user != null) {
        when (subject) {
            is Group -> sendMessage(MessageChainBuilder().append(At(user!!)).append(message).build())
            else -> sendMessage(message)
        }
    }
}

fun MessageEvent.getContact(): Contact {
    return when (this) {
        is GroupMessageEvent -> group
        else -> sender
    }
}


object ValorantBotPlugin : KotlinPlugin(
    description = JvmPluginDescription(
        id = "io.tiangou.valorant-bot-plugin",
        name = "valorant-bot-plugin",
        version = "0.5.1"
    )
    {
        author("xiaoxue1272")
    }
) {

    override fun onEnable() {
        Global.reload()
        CronTaskManager.reload()
        if (Global.databaseConfig.isInitOnEnable) {
            launch {
                PersistenceDataInitiator.init()
            }
        }
        EventHandler.registerTo(GlobalEventChannel)
        CronTaskManager.start()
        StoreCachesCleanTask.enable()
        CommandManager.registerCommand(CronTaskCommand, true)
        logger.info("valorant bot plugin enabled")
    }

    override fun onDisable() {
        CommandManager.unregisterCommand(CronTaskCommand)
        CronTaskManager.stop()
        EventHandler.cancelAll()
        logger.info("valorant bot plugin disabled")
    }

}

object Global : ReadOnlyPluginConfig("plugin-config") {

    @property:JvmStatic
    val json = Json {
        encodeDefaults = true
        useAlternativeNames = false
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val coroutineScope: CoroutineScope
        get() = ValorantBotPlugin

    @ValueDescription("事件监听配置")
    val eventConfig: EventConfigData by value(EventConfigData())

    @ValueDescription("数据库配置")
    val databaseConfig: DatabaseConfigData by value(DatabaseConfigData())

    @Serializable
    data class EventConfigData(
        @ValueDescription("当输入不正确时发出警告(为true时回复未知操作,false不回复消息)")
        val isWarnOnInputNotFound: Boolean = true,
        @ValueDescription(
            """
            配置群内消息监听策略: AT: 监听AT消息, AT_AND_QUOTE_REPLY: 监听AT和引用回复消息, QUOTE_REPLY: 监听引用回复消息, NONE: 不监听群内消息, ALL: 监听所有群消息
            默认为: AT_AND_QUOTE_REPLY, 就是监听 AT消息和引用回复消息.
        """
        )
        val groupMessageHandleStrategy: GroupMessageHandleEnum = GroupMessageHandleEnum.AT_AND_QUOTE_REPLY,
        @ValueDescription("指令等待输入超时时间,请不要设置太短或太长,分为单位,默认5分钟")
        val waitTimeoutMinutes: Int = 5
    )

    @Serializable
    data class DatabaseConfigData(
        @ValueDescription("数据库连接JDBC URL")
        val jdbcUrl: String = "jdbc:sqlite:${ValorantBotPlugin.dataFolder}${File.separator}ValorantPlugin.DB3",
        @ValueDescription("是否在插件加载时就初始化数据库数据")
        val isInitOnEnable: Boolean = true
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


open class ValorantRuntimeException(override val message: String?) : RuntimeException()



