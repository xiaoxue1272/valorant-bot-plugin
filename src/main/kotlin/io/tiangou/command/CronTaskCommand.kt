package io.tiangou.command

import io.tiangou.ValorantBotPlugin
import io.tiangou.checkPermission
import io.tiangou.cron.CronTaskManager
import io.tiangou.cron.Task
import io.tiangou.reply
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.message.data.MessageChainBuilder

object CronTaskCommand : CompositeCommand(
    ValorantBotPlugin,
    "task",
    description = "Valorant Plugin Task Operations"
) {

    private suspend fun getTaskByString(context: CommandContext, arg: String): Task? {
        val task = CronTaskManager.taskList.find { it::class.simpleName!!.uppercase() == arg.uppercase() }
        if (task == null) {
            context.sender.reply("未找到符合的任务,请检查输入")
        }
        return task
    }

    @SubCommand("list", "all")
    @Description("定时任务列表")
    suspend fun list(context: CommandContext) {
        if (!context.checkPermission()) {
            return
        }
        val builder = MessageChainBuilder()
        CronTaskManager.taskList.forEach {
            builder.append("[${it::class.simpleName!!}] [${it.description}] isEnable: ${it.isEnable}\n")
        }
        context.sender.reply(builder.build())
    }

    @SubCommand("trigger")
    @Description("手动触发定时任务")
    suspend fun trigger(context: CommandContext, arg: String) {
        if (!context.checkPermission()) {
            return
        }
        getTaskByString(context, arg)?.executeRunCaching()
    }

    @SubCommand("update")
    @Description("更新定时任务cron表达式")
    suspend fun update(context: CommandContext, arg: String) {
        getTaskByString(context, arg)?.apply {
            cron = arg
            if (isEnable) {
                disable()
                enable()
            }
        }
    }


    @SubCommand("enable")
    @Description("启用定时任务")
    suspend fun enable(context: CommandContext, arg: String) {
        if (!context.checkPermission()) {
            return
        }
        getTaskByString(context, arg)?.apply { isEnable = true }?.enable()
    }

    @SubCommand("disable")
    @Description("禁用定时任务")
    suspend fun disable(context: CommandContext, arg: String) {
        if (!context.checkPermission()) {
            return
        }
        getTaskByString(context, arg)?.apply { isEnable = false }?.disable()
    }

}