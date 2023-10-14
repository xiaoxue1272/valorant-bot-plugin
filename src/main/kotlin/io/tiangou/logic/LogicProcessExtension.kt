package io.tiangou.logic

import io.tiangou.config.PluginConfig
import net.mamoe.mirai.event.events.MessageEvent

internal val default_logic_list: List<Pair<String, List<LogicProcessor<MessageEvent>>>> by lazy {
    listOf(
        "帮助" to listOf(HelpListLogicProcessor),
        "账号登录" to listOf(LoginRiotAccountLogicProcessor),
        "设置地区" to listOf(ChangeLocationShardLogicProcessor),
        "查询商店" to listOf(
            CheckRiotStatusAndSettingProcessor,
            QueryPlayerDailyStoreProcessor
        ),
        "自定义商店背景图" to listOf(UploadCustomBackgroundProcessor),
        "查询配件商店" to listOf(CheckRiotStatusAndSettingProcessor, QueryPlayerAccessoryStoreProcessor),
        "群推送设置" to listOf(
            CheckRiotStatusAndSettingProcessor,
            CheckIsBotFriendProcessor,
            DesignateSubscribeSettingProcessor
        ),
        "当前聊天推送设置" to listOf(
            CheckRiotStatusAndSettingProcessor,
            CheckIsBotFriendProcessor,
            CurrentSubscribeSettingProcessor
        )
    )
}

internal val default_help_list: String by lazy {
    "帮助列表\n" +
            default_logic_list.mapIndexed { index, it -> "${index + 1}.${it.first}" }.joinToString("\n") +
            "\n退出指令请发送: ${PluginConfig.eventConfig.exitLogicCommand}"
}


internal val ASK_LOCATION_AREA_MESSAGE by lazy {
    """
    请输入你想要设置的地区
    
    亚洲
    北美
    巴西
    拉丁美洲
    韩国
    欧洲
    
    请输入正确的地区值
    """.trimIndent()
}

enum class ServerLocationEnum(
    val value: String,
    val shard: String,
    val region: String
) {
    AP("亚洲", "ap", "ap"),
    NA("北美", "na", "na"),
    BR("巴西", "na", "br"),
    LATAM("拉丁美洲", "na", "latam"),
    KR("韩国", "kr", "kr"),
    EU("欧洲", "eu", "eu"),
    ;
}