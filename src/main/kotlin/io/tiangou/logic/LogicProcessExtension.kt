package io.tiangou.logic

import io.tiangou.ValueEnum
import io.tiangou.config.PluginConfig
import io.tiangou.toValueFieldString
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder

internal val default_logic_list: List<Pair<String, List<LogicProcessor<MessageEvent>>>> by lazy {
    listOf(
        "帮助" to listOf(
            HelpListLogicProcessor
        ),
        "账号登录" to listOf(
            LoginRiotAccountLogicProcessor
        ),
        "退出登录" to listOf(
            LogoutRiotAccountLogicProcessor
        ),
        "删除用户数据" to listOf(
            DeleteUserCacheLogicProcessor
        ),
        "设置地区" to listOf(ChangeLocationShardLogicProcessor),
        "自定义商店背景图" to listOf(UploadCustomBackgroundLogicProcessor),
        "查询商店" to listOf(
            CheckRiotStatusLogicProcessor,
            QueryDailyStoreLogicProcessor
        ),
        "查询配件商店" to listOf(
            CheckRiotStatusLogicProcessor,
            QueryAccessoryStoreLogicProcessor
        ),
//        "查询夜市" to listOf(
// todo
//        ),
        "群推送设置" to listOf(
            CheckRiotStatusLogicProcessor,
            DesignateSubscribeSettingLogicProcessor
        ),
        "查看群推送设置" to  listOf(
            CheckRiotStatusLogicProcessor,
            ViewDesignateSubscribeSettingLogicProcessor
        ),
        "当前聊天推送设置" to listOf(
            CheckRiotStatusLogicProcessor,
            CurrentSubscribeSettingLogicProcessor
        ),
        "查看当前聊天推送设置" to listOf(
            CheckRiotStatusLogicProcessor,
            ViewCurrentSubscribeSettingLogicProcessor
        ),
        "查看所有推送设置" to listOf(
            CheckRiotStatusLogicProcessor,
            ViewAllSubscribeSettingLogicProcessor
        )
    )
}

internal val default_help_list: String by lazy {
    "帮助列表\n" +
            default_logic_list.mapIndexed { index, it -> "${index + 1}.${it.first}" }.joinToString("\n") +
            "\n退出指令请发送: ${PluginConfig.eventConfig.exitLogicCommand}"
}

internal inline fun <reified E> askEnumMessage() where E : Enum<E>, E : ValueEnum =
    MessageChainBuilder()
        .append(toValueFieldString<E>())
        .append("\n请输入正确的指令或汉字")
        .build()


