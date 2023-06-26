package io.tiangou

import io.tiangou.cron.CronTaskManager
import io.tiangou.repository.ValorantThirdPartyPersistenceDataInitiator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content


const val VERSION = "0.4.1-pre"

fun MessageChain.toText() =
    MessageChainBuilder().apply { addAll(this@toText.filterIsInstance<PlainText>()) }.asMessageChain().content.trim()


object ValorantBotPlugin : KotlinPlugin(
    description = JvmPluginDescription(
        id = "io.tiangou.valorant-bot-plugin",
        name = "valorant-bot-plugin",
        version = VERSION
    )
    {
        author("xiaoxue1272")
    }
) {

    override fun onEnable() {
        CronTaskManager.reload()
        runBlocking { ValorantThirdPartyPersistenceDataInitiator.init() }
        CronTaskManager.start()
        EventHandler.registerTo(GlobalEventChannel)
        logger.info("valorant bot plugin enabled")
    }

    override fun onDisable() {
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



