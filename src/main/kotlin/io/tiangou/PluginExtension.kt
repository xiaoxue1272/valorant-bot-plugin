@file:UseSerializers(PathSerializer::class)
@file:Suppress("unused")

package io.tiangou

import io.tiangou.command.CronTaskCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
import net.mamoe.mirai.console.util.cast
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
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

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
    ) { it.sender.id == sender.id && it.toText() != Global.eventConfig.exitLogicCommand }


suspend fun MessageEvent.reply(message: String) {
    when (this) {
        is GroupMessageEvent -> group.sendMessage(
            MessageChainBuilder().append(At(sender)).append("\n").append(message).build()
        )

        else -> sender.sendMessage(message)
    }
}

suspend fun MessageEvent.reply(message: Message) {
    when (this) {
        is GroupMessageEvent -> group.sendMessage(
            MessageChainBuilder().append(At(sender)).append("\n").append(message).build()
        )

        else -> sender.sendMessage(message)
    }
}

suspend fun Contact.reply(message: String, target: User? = null) =
    when (this) {
        is Group -> {
            if (target != null) {
                if (contains(target.id))
                    sendMessage(
                        MessageChainBuilder().append(target.cast<Member>().at()).append("\n").append(message).build()
                    )
                else null
            } else sendMessage(MessageChainBuilder().append(message).build())
        }

        else -> sendMessage(message)
    }

suspend fun Contact.reply(message: Message, target: User? = null) =
    when (this) {
        is Group -> {
            if (target != null) {
                if (contains(target.id))
                    sendMessage(
                        MessageChainBuilder().append(target.cast<Member>().at()).append("\n").append(message).build()
                    )
                else null
            } else sendMessage(MessageChainBuilder().append(message).build())
        }

        else -> sendMessage(message)
    }

internal suspend fun MessageEvent.isVisitAllow(qq: Long): Boolean {
    if (!isVisitAllow(qq, bot)) reply("[$qq],暂无访问权限")
    return true
}

internal suspend fun MessageEvent.isVisitAllow(): Boolean {
    isVisitAllow(sender.id)
    return true
}

fun isVisitAllow(qq: Long, bot: Bot): Boolean {
    val contact: Contact = bot.getContact(qq) ?: return false
    when (VisitConfig.controlType) {
        VisitControlEnum.WHITE_LIST -> if (!contact.getVisitControlList().contains(qq)) {
            return false
        }

        VisitControlEnum.BLACK_LIST -> if (contact.getVisitControlList().contains(qq)) {
            return false
        }
    }
    return true
}

fun Contact.getVisitControlList() = when (this) {
    is Group -> VisitConfig.onGroups
    else -> VisitConfig.onUsers
}

suspend fun CommandSender.reply(message: String) {
    if (subject != null && subject is Group) {
        sendMessage(
            MessageChainBuilder()
                .apply { if (user != null) append(At(user!!)).append("\n") }
                .append(message)
                .build()
        )
    } else sendMessage(message)
}

suspend fun CommandSender.reply(message: Message) {
    if (subject != null && subject is Group) {
        sendMessage(
            MessageChainBuilder()
                .apply { if (user != null) append(At(user!!)).append("\n") }
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

suspend fun MessageEvent.replyImage(bytes: ByteArray) {
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

    val configFolder: File = ValorantBotPlugin.configFolder

    val configFolderPath: Path = ValorantBotPlugin.configFolderPath

    val dataFolder: File = ValorantBotPlugin.dataFolder

    val dataFolderPath: Path = ValorantBotPlugin.dataFolderPath

    val pluginCacheFolder: File = ValorantBotPlugin.dataFolder.resolve("cache").apply { mkdir() }

    val pluginCacheFolderPath: Path = pluginCacheFolder.toPath()

    @ValueDescription("事件监听配置")
    val eventConfig: EventConfigData by value(EventConfigData())

    @ValueDescription("数据库配置")
    val databaseConfig: DatabaseConfigData by value(DatabaseConfigData())

    @ValueDescription("绘图相关配置")
    val drawImageConfig: DrawImageConfig by value(DrawImageConfig())

    @Serializable
    data class EventConfigData(
        @ValueDescription("当输入不正确时发出警告(为true时回复未知操作,false不回复消息)")
        val isWarnOnInputNotFound: Boolean = true,
        @ValueDescription(
            """
            配置群内消息监听策略
            AT: 监听AT消息, 
            AT_AND_QUOTE_REPLY: 监听AT和引用回复消息, 
            QUOTE_REPLY: 监听引用回复消息, 
            NONE: 不监听群内消息, ALL: 监听所有群消息
            默认为: AT_AND_QUOTE_REPLY, 就是监听 AT消息和引用回复消息.
        """
        )
        val groupMessageHandleStrategy: GroupMessageHandleEnum = GroupMessageHandleEnum.AT_AND_QUOTE_REPLY,
        @ValueDescription("是否自动接受好友申请")
        val autoAcceptFriendRequest: Boolean = true,
        @ValueDescription("是否自动接受群邀请")
        val autoAcceptGroupRequest: Boolean = true,
        @ValueDescription("退出指令执行的关键字 默认为[退出]")
        val exitLogicCommand: String = "退出"
    )

    @Serializable
    data class DatabaseConfigData internal constructor(
        @ValueDescription("数据库连接JDBC URL")
        val jdbcUrl: String = "jdbc:sqlite:${ValorantBotPlugin.dataFolder}${File.separator}ValorantPlugin.DB3",
        @ValueDescription("是否在插件加载时就初始化数据库数据")
        val isInitOnEnable: Boolean = true
    )

    @Serializable
    data class DrawImageConfig internal constructor(
        @ValueDescription(
            """
                SKIKO: 使用Skiko进行绘图(某些CPU架构可能不支持)
                AWT: 使用Java内置GUI进行绘图
                默认为: SKIKO
            """
        )
        val api: DrawImageApiEnum = DrawImageApiEnum.SKIKO,
        @ValueDescription("字体相关配置")
        val font: FontConfigData = FontConfigData(),
        @ValueDescription("GUI库类存放路径(目前仅SKIKO使用,AWT不使用)")
        val libDictionary: Path = PluginManager.pluginLibrariesPath,
        @ValueDescription("背景图片相关配置")
        val background: BackgroundConfigData = BackgroundConfigData(),
        @ValueDescription("生成的图片缓存存放类型 内存 or 磁盘")
        val cache: CacheType = CacheType.MEMORY
    ) {

        @Serializable
        data class FontConfigData internal constructor(
            @ValueDescription("字体资源路径配置")
            val reference: ResourceResolveConfigData = ResourceResolveConfigData(),
            @ValueDescription("字体颜色 默认白色")
            val color: String = "#FFFFFF",
        )

        @Serializable
        data class BackgroundConfigData internal constructor(
            @ValueDescription("默认背景资源路径配置")
            val reference: ResourceResolveConfigData = ResourceResolveConfigData(
                ResourceReferenceType.URL,
                "https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg"
            ),
            @ValueDescription("背景图片不透明度")
            val alpha: Double = 1.0
        ) {

            init {
                reference.getResourceBytes()
                    ?: reference.apply {
                        type = ResourceReferenceType.URL
                        value = "https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg"
                        initResource()
                    }
            }

        }


    }

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
enum class DrawImageApiEnum {
    AWT,
    SKIKO,
}

@Serializable
data class ResourceResolveConfigData internal constructor(
    @ValueDescription(
        """
                背景图片的地址类型
                PATH: 本地磁盘下的路径
                URL: Url类型的路径 (最常见的也就是 http://xxx.xxx, https://xxx.xxx, file://xxx/xxx)
                当类型为URL时,会在本插件的 data/cache/ 目录下缓存默认背景图片.
                主要目的是节省一些可能的非必要网络IO 并且会将 type修改为 PATH, value 也会修改为文件对应的绝对路径.
            """
    )
    var type: ResourceReferenceType? = null,
    @ValueDescription("资源路径值")
    var value: String? = null
) {

    @Transient
    private var resource: File? = null

    init {
        initResource()
    }

    fun initResource() {
        resource = when (type) {
            ResourceReferenceType.URL -> value.takeIf { it?.isNotEmpty() == true }?.let {
                val url = URI.create(it).toURL()
                Global.pluginCacheFolder.resolve(url.file.substringAfterLast("/")).apply {
                    createNewFile()
                    writeBytes(url.readBytes())
                    type = ResourceReferenceType.PATH
                    value = absolutePath
                }
            }

            ResourceReferenceType.PATH -> value?.let { Path(it).toFile() }
            null -> null
        }
    }

    fun getResourceFile() = resource

    fun getResourceBytes(): ByteArray? = getResourceFile()?.readBytes()

}

@Serializable
enum class ResourceReferenceType {
    URL,
    PATH
}

@Serializable
enum class CacheType {
    MEMORY,
    FILE,
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
enum class VisitControlEnum {
    WHITE_LIST,
    BLACK_LIST
}

open class ValorantPluginException(override val message: String?) : Exception()