@file:UseSerializers(PathSerializer::class)
@file:Suppress("unused")

package io.tiangou

import io.tiangou.command.CronTaskCommand
import io.tiangou.config.PluginConfig
import io.tiangou.config.VisitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.nextEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.isUploaded
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.nio.file.Path


fun MessageChain.toText() = filterIsInstance<PlainText>().toMessageChain().content.trim()

fun MessageEvent.toText() = message.toText()

fun getOnlineBots() = Bot.instances.filter { it.isOnline }

fun Bot.getUser(qq: Long) = getFriend(qq) ?: getStranger(qq)

fun Bot.getUserOrFail(qq: Long) = getFriend(qq) ?: getStranger(qq) ?: throw NoSuchElementException("User $id")

fun Bot.getContact(qq: Long) = getUser(qq) ?: getGroup(qq)

fun Bot.getContactOrFail(qq: Long) = getUser(qq) ?: getGroup(qq) ?: throw NoSuchElementException("Contact $id")

suspend fun MessageEvent.nextMessageEvent(): MessageEvent =
    GlobalEventChannel.nextEvent(
        EventPriority.HIGHEST,
        intercept = true
    ) { it.sender.id == sender.id && it.toText() != PluginConfig.eventConfig.exitLogicCommand }


suspend infix fun MessageEvent.reply(message: String) {
    when (this) {
        is GroupMessageEvent -> group.sendMessage(
            MessageChainBuilder().append(sender.at()).append("\n").append(message).build()
        )

        else -> sender.sendMessage(message)
    }
}

suspend infix fun MessageEvent.reply(message: Message) {
    when (this) {
        is GroupMessageEvent -> group.sendMessage(
            MessageChainBuilder().append(sender.at()).append("\n").append(message).build()
        )

        else -> sender.sendMessage(message)
    }
}

suspend fun Contact.reply(message: String, target: Member? = null) =
    when (this) {
        is Group -> sendMessage(
            MessageChainBuilder().apply { if (target != null) append(At(target)) }.append("\n").append(message).build()
        )

        else -> sendMessage(message)
    }

suspend fun Contact.reply(message: Message, target: User? = null) =
    when (this) {
        is Group -> sendMessage(
            MessageChainBuilder().apply { if (target != null) append(At(target)) }.append("\n").append(message).build()
        )

        else -> sendMessage(message)
    }

suspend fun Contact.reply(message: String, target: Long) =
    when (this) {
        is Group -> sendMessage(MessageChainBuilder().append(At(target)).append("\n").append(message).build())

        else -> sendMessage(message)
    }

suspend fun Contact.reply(message: Message, target: Long) =
    when (this) {
        is Group -> sendMessage(MessageChainBuilder().append(At(target)).append("\n").append(message).build())

        else -> sendMessage(message)
    }

suspend infix fun Contact.reply(message: String) = reply(message, null)

suspend infix fun Contact.reply(message: Message) = reply(message, null)

internal suspend infix fun MessageEvent.isVisitAllow(qq: Long): Boolean {
    if (!isVisitAllow(qq, bot)) {
        reply("[$qq],暂无访问权限")
        return false
    }
    return true
}

internal suspend fun MessageEvent.isVisitAllow(): Boolean = isVisitAllow(sender.id)

fun isVisitAllow(qq: Long, bot: Bot): Boolean {
    val contact: Contact = bot.getContact(qq) ?: return false
    when (VisitConfig.controlType) {
        VisitConfig.VisitControlEnum.WHITE_LIST -> if (!contact.getVisitControlList().contains(qq)) {
            return false
        }

        VisitConfig.VisitControlEnum.BLACK_LIST -> if (contact.getVisitControlList().contains(qq)) {
            return false
        }
    }
    return true
}

fun Contact.getVisitControlList() = when (this) {
    is Group -> VisitConfig.onGroups
    else -> VisitConfig.onUsers
}

suspend infix fun CommandSender.reply(message: String) {
    if (subject != null && subject is Group) {
        sendMessage(
            MessageChainBuilder()
                .apply { if (user != null) append(At(user!!)).append("\n") }
                .append(message)
                .build()
        )
    } else sendMessage(message)
}

suspend infix fun CommandSender.reply(message: Message) {
    if (subject != null && subject is Group) {
        sendMessage(
            MessageChainBuilder()
                .apply { if (user != null) append(At(user!!)).append("\n") }
                .append(message)
                .build()
        )
    } else sendMessage(message)
}

suspend fun uploadImage(bytes: ByteArray, contact: Contact): Image? {
    var uploadImage = contact.uploadImage(bytes.toExternalResource().toAutoCloseable())
    repeat(2) {
        if (!uploadImage.isUploaded(contact.bot)) {
            uploadImage =
                contact.uploadImage(bytes.toExternalResource().toAutoCloseable())
        }
    }
    return uploadImage.isUploaded(contact.bot).takeIf { it }?.let { uploadImage }
}

suspend fun uploadImageOrFail(bytes: ByteArray, contact: Contact) =
    uploadImage(bytes, contact) ?: throw RuntimeException("图片上传失败,请稍候重试")

suspend fun Contact.uploadImage(bytes: ByteArray) = uploadImage(bytes, this)

suspend fun Contact.uploadImageOrFail(bytes: ByteArray) = uploadImageOrFail(bytes, this)

suspend infix fun MessageEvent.replyImage(bytes: ByteArray) {
    uploadImage(bytes, subject)?.apply {
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

object PluginGlobal {

    val coroutineScope: CoroutineScope
        get() = ValorantBotPlugin

    val configFolder: File = ValorantBotPlugin.configFolder

    val configFolderPath: Path = ValorantBotPlugin.configFolderPath

    val dataFolder: File = ValorantBotPlugin.dataFolder

    val dataFolderPath: Path = ValorantBotPlugin.dataFolderPath

    val pluginCacheFolder: File = ValorantBotPlugin.dataFolder.resolve("cache").apply { mkdir() }

    val pluginCacheFolderPath: Path = pluginCacheFolder.toPath()

}

open class ValorantPluginException(override val message: String?) : Exception()

/**
 * 生成图片类型
 */
enum class GenerateImageType(val value: String) {

    SKINS_PANEL_LAYOUT("每日商店"),

    // 每周三早上8点刷新
    ACCESSORY_STORE("配件商店"),

}

/**
 * 订阅定时任务类型
 */
@Serializable
enum class SubscribeType(
    val value: String
) {
    DAILY_STORE("每日商店"),
    WEEKLY_ACCESSORY_STORE("每周配件商店"),
    DAILY_MATCH_SUMMARY_DATA("每日比赛数据统计");


    companion object {

        fun findByValue(value: String): SubscribeType? = SubscribeType.values().firstOrNull { it.value == value }

        fun findByValueNotNull(value: String): SubscribeType =
            findByValue(value) ?: throw ValorantPluginException("无效的订阅类型")

        fun findByName(name: String): SubscribeType? = SubscribeType.values().firstOrNull { it.name == name }

        fun findByNameNotNull(name: String): SubscribeType =
            findByName(name) ?: throw ValorantPluginException("无效的订阅类型")

        fun find(keywords: String): SubscribeType? =
            SubscribeType.values().firstOrNull { it.name == keywords || it.value == keywords }

        fun findNotNull(keywords: String): SubscribeType =
            find(keywords) ?: throw ValorantPluginException("无效的订阅类型")

        fun all() = values().toMutableList()

    }


}