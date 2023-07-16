package io.tiangou.command

import io.tiangou.*
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CompositeCommand

object VisitCommand: CompositeCommand(
    ValorantBotPlugin,
    "visit",
    description = "Valorant Plugin Visit Operations"
) {

    @SubCommand("groups")
    @Description("查看onGroups中的群")
    suspend fun groups(context: CommandContext) {
        if (!context.checkPermission()) {
            return
        }
        context.sender.reply(VisitConfig.onGroups.joinToString("\n"))
    }

    @SubCommand("users")
    @Description("查看onUsers中的用户")
    suspend fun users(context: CommandContext) {
        if (!context.checkPermission()) {
            return
        }
        context.sender.reply(VisitConfig.onUsers.joinToString("\n"))
    }

    @SubCommand("control")
    @Description("查看当前方访问控制类型")
    suspend fun control(context: CommandContext) {
        if (!context.checkPermission()) {
            return
        }
        context.sender.reply(VisitConfig.controlType.name)
    }

    @SubCommand("all")
    @Description("查看当前所有设置")
    suspend fun all(context: CommandContext) {
        if (!context.checkPermission()) {
            return
        }
        context.sender.reply("""
            controlType: ${VisitConfig.controlType}
            onUsers: ${VisitConfig.onUsers.joinToString("\n")}
            onGroups: ${VisitConfig.onGroups.joinToString("\n")}
        """.trimIndent())
    }

    @SubCommand("change")
    @Description("修改访问类型 当前为白名单则更新为黑名单,反之同理")
    suspend fun change(context: CommandContext, needClear: Boolean = false) {
        if (!context.checkPermission()) {
            return
        }
        VisitConfig.controlType = when(VisitConfig.controlType) {
            VisitControlEnum.WHITE_LIST -> VisitControlEnum.BLACK_LIST
            VisitControlEnum.BLACK_LIST -> VisitControlEnum.WHITE_LIST
        }
        if (needClear) {
            VisitConfig.onGroups.clear()
            VisitConfig.onUsers.clear()
        }
    }

    @SubCommand("add group")
    @Description("将给群添加到访问控制中")
    suspend fun addGroup(context: CommandContext, group: Long) {
        if (!context.checkPermission()) {
            return
        }
        VisitConfig.onGroups.add(group)
    }

    @SubCommand("add user")
    @Description("将给定用户添加到访问控制中")
    suspend fun addUser(context: CommandContext, user: Long) {
        if (!context.checkPermission()) {
            return
        }
        VisitConfig.onUsers.add(user)
    }

    @SubCommand("remove group")
    @Description("将给定群从访问控制中删除")
    suspend fun removeGroup(context: CommandContext, group: Long) {
        if (!context.checkPermission()) {
            return
        }
        VisitConfig.onGroups.remove(group)
    }

    @SubCommand("remove user")
    @Description("将给定用户从访问控制中删除")
    suspend fun removeUser(context: CommandContext, user: Long) {
        if (!context.checkPermission()) {
            return
        }
        VisitConfig.onUsers.remove(user)
    }

}