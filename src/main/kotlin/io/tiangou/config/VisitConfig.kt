package io.tiangou.config

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object VisitConfig : AutoSavePluginConfig("visit-config") {

    @Serializable
    enum class VisitControlEnum {
        WHITE_LIST,
        BLACK_LIST
    }

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