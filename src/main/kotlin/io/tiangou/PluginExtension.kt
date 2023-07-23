@file:UseSerializers(PathSerializer::class)

package io.tiangou

import io.tiangou.command.CronTaskCommand
import io.tiangou.serializer.PathSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.PluginManager
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.isUploaded
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.nio.file.Path

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

suspend fun Contact.reply(message: String, target: User) {
    when (this) {
        is Group -> sendMessage(MessageChainBuilder().append(At(target)).append(message).build())
        else -> sendMessage(message)
    }
}

suspend fun Contact.reply(message: Message, target: User) {
    when (this) {
        is Group -> sendMessage(MessageChainBuilder().append(At(target)).append(message).build())
        else -> sendMessage(message)
    }
}

fun isVisitAllow(qq: Long, bot: Bot): Boolean {
    if (bot.getGroup(qq) == null) {
        return false
    }
    when (VisitConfig.controlType) {
        VisitControlEnum.WHITE_LIST -> if (!VisitConfig.onGroups.contains(qq)) {
            return false
        }

        VisitControlEnum.BLACK_LIST -> if (VisitConfig.onGroups.contains(qq)) {
            return false
        }
    }
    return true
}

suspend fun CommandSender.reply(message: String) {
    if (subject != null && subject is Group) {
        sendMessage(
            MessageChainBuilder()
                .apply { if (user != null) append(At(user!!)) }
                .append(message)
                .build()
        )
    } else sendMessage(message)
}

suspend fun CommandSender.reply(message: Message) {
    if (subject != null && subject is Group) {
        sendMessage(
            MessageChainBuilder()
                .apply { if (user != null) append(At(user!!)) }
                .append(message)
                .build()
        )
    } else sendMessage(message)
}

suspend fun uploadImage(bytes: ByteArray, contact: Contact, bot: Bot): Image? {
    var uploadImage = contact.uploadImage(bytes.toExternalResource().toAutoCloseable())
    repeat(2) {
        if (!uploadImage.isUploaded(bot)) {
            uploadImage =
                contact.uploadImage(bytes.toExternalResource().toAutoCloseable())
        }
    }
    return uploadImage.isUploaded(bot).takeIf { it }?.let { uploadImage }
}

suspend fun MessageEvent.uploadImage(bytes: ByteArray) {
    uploadImage(bytes, subject, bot)?.apply {
        reply(MessageChainBuilder().append(this).build())
    } ?: reply("图片上传失败,请稍候重试")
}

suspend fun CommandContext.checkPermission(): Boolean {
    if (!sender.hasPermission(CronTaskCommand.permission)) {
        sender.reply("暂无权限")
        return false
    }
    return true
}

object Global : ReadOnlyPluginConfig("plugin-config") {

    val json = Json {
        encodeDefaults = true
        useAlternativeNames = false
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val coroutineScope: CoroutineScope
        get() = ValorantBotPlugin

    @ValueDescription(
        """
        事件监听配置
        isWarnOnInputNotFound: 当输入不正确时发出警告(为true时回复未知操作,false不回复消息)
        groupMessageHandleStrategy: 配置群内消息监听策略
            AT: 监听AT消息, 
            AT_AND_QUOTE_REPLY: 监听AT和引用回复消息, 
            QUOTE_REPLY: 监听引用回复消息, 
            NONE: 不监听群内消息, ALL: 监听所有群消息
            默认为: AT_AND_QUOTE_REPLY, 就是监听 AT消息和引用回复消息.
        waitTimeoutMinutes: 指令等待输入超时时间,请不要设置太短或太长,分为单位,默认5分钟
        autoAcceptFriendRequest: 是否自动接受好友申请
        autoAcceptGroupRequest: 是否自动接受群邀请
        exitLogicCommand: 退出指令执行的关键字 默认为[退出]
    """
    )
    val eventConfig: EventConfigData by value(EventConfigData())

    @ValueDescription(
        """
        数据库配置
        jdbcUrl: 数据库连接JDBC URL
        isInitOnEnable: 是否在插件加载时就初始化数据库数据
    """
    )
    val databaseConfig: DatabaseConfigData by value(DatabaseConfigData())

    @ValueDescription(
        """
        绘图相关配置
        api: 
            SKIKO: 使用Skiko进行绘图(某些CPU架构可能不支持)
            AWT: 使用Java内置GUI进行绘图
            默认为: SKIKO
        libDictionary: GUI库类存放路径(目前仅SKIKO使用,AWT不使用)
    """
    )
    val drawImageConfig: DrawImageConfig by value(DrawImageConfig())

    @Serializable
    data class EventConfigData(
        val isWarnOnInputNotFound: Boolean = true,
        val groupMessageHandleStrategy: GroupMessageHandleEnum = GroupMessageHandleEnum.AT_AND_QUOTE_REPLY,
        val waitTimeoutMinutes: Int = 5,
        val autoAcceptFriendRequest: Boolean = true,
        val autoAcceptGroupRequest: Boolean = true,
        val exitLogicCommand: String = "退出"
    )

    @Serializable
    data class DatabaseConfigData(
        val jdbcUrl: String = "jdbc:sqlite:${ValorantBotPlugin.dataFolder}${File.separator}ValorantPlugin.DB3",
        val isInitOnEnable: Boolean = true
    )

    @Serializable
    data class DrawImageConfig(
        val api: DrawImageApiEnum = DrawImageApiEnum.AWT,
        val libDictionary: Path = PluginManager.pluginLibrariesPath
    )

}

object VisitConfig : AutoSavePluginConfig("visit-config") {

    @ValueDescription(
        """
        访问控制(比如说插件指定插件仅对某些人/群内生效, 或者仅排除掉某些用户/群)
            WHITE_LIST: 白名单模式 (仅onGroup, onUsers中配置的群/用户可以使用本插件)
            BLACK_LIST: 黑名单模式 (仅onGroup, onUsers中配置的群/用户不可以使用本插件)
    """
    )
    var controlType: VisitControlEnum by value(VisitControlEnum.BLACK_LIST)

    @ValueDescription("访问控制作用的群集合 默认为空")
    val onGroups: MutableList<Long> by value()

    @ValueDescription("访问控制作用的群集合 默认为空")
    val onUsers: MutableList<Long> by value()
}

@Serializable
enum class GroupMessageHandleEnum {
    AT,
    QUOTE_REPLY,
    AT_AND_QUOTE_REPLY,
    NONE,
    ALL
}

@Serializable
enum class VisitControlEnum {
    WHITE_LIST,
    BLACK_LIST
}

@Serializable
enum class DrawImageApiEnum {
    AWT,
    SKIKO,
}

open class ValorantRuntimeException(override val message: String?) : RuntimeException()