package io.tiangou

import io.tiangou.command.CronTaskCommand
import io.tiangou.command.VisitCommand
import io.tiangou.cron.CachesCleanCronTask
import io.tiangou.cron.CronTaskManager
import io.tiangou.repository.persistnce.PersistenceDataInitiator
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo


object ValorantBotPlugin : KotlinPlugin(
    description = JvmPluginDescription(
        id = "io.tiangou.valorant-bot-plugin",
        name = "valorant-bot-plugin",
        version = "0.8.0-dev"
    )
    {
        author("xiaoxue1272")
    }
) {

    override fun onEnable() {
        Global.reload()
        VisitConfig.reload()
        CronTaskManager.apply { reload(); start() }
        CachesCleanCronTask.enable()
        runBlocking {
            if (Global.databaseConfig.isInitOnEnable) {
                PersistenceDataInitiator.init()
            }
            GuiLibraryLoader.loadApi(Global.drawImageConfig.api)
        }
        CommandManager.registerCommand(CronTaskCommand, false)
        CommandManager.registerCommand(VisitCommand, false)
        EventHandler.registerTo(GlobalEventChannel)
        logger.info("valorant bot plugin enabled")
    }

    override fun onDisable() {
        CommandManager.unregisterCommand(CronTaskCommand)
        CronTaskManager.stop()
        EventHandler.cancelAll()
        logger.info("valorant bot plugin disabled")
    }

}



