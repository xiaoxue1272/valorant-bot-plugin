package io.tiangou

import io.tiangou.cron.CronTaskCommand
import io.tiangou.cron.CronTaskManager
import io.tiangou.cron.StoreCachesCleanTask
import io.tiangou.repository.PersistenceDataInitiator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.message.data.*


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
        version = "0.5.0-pre"
    )
    {
        author("xiaoxue1272")
    }
) {

    override fun onEnable() {
        EventHandleConfig.reload()
        CronTaskManager.reload()
        runBlocking { PersistenceDataInitiator.init() }
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

object Global {
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

}


open class ValorantRuntimeException(override val message: String?) : RuntimeException()



