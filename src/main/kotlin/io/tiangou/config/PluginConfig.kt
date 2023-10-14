package io.tiangou.config

import io.tiangou.PluginGlobal
import io.tiangou.ValorantBotPlugin
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.PluginManager
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

object PluginConfig : ReadOnlyPluginConfig("plugin-config") {

    @ValueDescription("事件监听配置")
    val eventConfig: EventConfigData by value(EventConfigData())

    @ValueDescription("数据库配置")
    val databaseConfig: DatabaseConfigData by value(DatabaseConfigData())

    @ValueDescription("绘图相关配置")
    val drawImageConfig: DrawImageConfig by value(DrawImageConfig())

    @ValueDescription("用户未登录状态下保留Riot账号信息的天数")
    val logoutRiotAccountCleanDay: Int by value(7)

    @ValueDescription("用户未登录状态下保留用户信息的天数")
    val logoutUserCacheCleanDay: Int by value(30)

    @ValueDescription("每日商店, 配件商店绘图比例大小")
    val storeImageWidthHeightProportion: Pair<Int, Int> by value(9 to 16)

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
    ) {

        @Serializable
        enum class GroupMessageHandleEnum {
            AT,
            QUOTE_REPLY,
            AT_AND_QUOTE_REPLY,
            NONE,
            ALL
        }

    }

    @Serializable
    data class DatabaseConfigData internal constructor(
        @ValueDescription("数据库连接JDBC URL")
        val jdbcUrl: String = "jdbc:sqlite:${ValorantBotPlugin.dataFolder}${File.separator}ValorantPlugin.DB3",
        @ValueDescription("是否在插件加载时就初始化数据库数据")
        val isInitOnEnable: Boolean = true
    )

    @Serializable
    enum class CacheType {
        MEMORY,
        FILE,
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

        @Serializable
        enum class ResourceReferenceType {
            URL,
            PATH
        }

        @Transient
        private var resource: File? = null

        init {
            initResource()
        }

        fun initResource() {
            resource = when (type) {
                ResourceReferenceType.URL -> value.takeIf { it?.isNotEmpty() == true }?.let {
                    val url = URI.create(it).toURL()
                    PluginGlobal.pluginCacheFolder.resolve(url.file.substringAfterLast("/")).apply {
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
        enum class DrawImageApiEnum {
            AWT,
            SKIKO,
        }

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
                ResourceResolveConfigData.ResourceReferenceType.URL,
                "https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg"
            ),
            @ValueDescription("背景图片不透明度")
            val alpha: Double = 1.0
        ) {

            init {
                reference.getResourceBytes()
                    ?: reference.apply {
                        type = ResourceResolveConfigData.ResourceReferenceType.URL
                        value = "https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg"
                        initResource()
                    }
            }

        }


    }

}