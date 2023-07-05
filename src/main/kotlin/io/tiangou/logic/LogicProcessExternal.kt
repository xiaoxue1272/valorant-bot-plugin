package io.tiangou.logic

val HELP_LIST_MESSAGE: String by lazy {
    """
    帮助列表
    1.账号登录 (注:请私聊发送)
               
    2.设置地区
               
    3.查询商店
    
    4.更新每日商店推送任务状态
    
    5.自定义商店背景图
    
    6.查询配件商店
    """.trimIndent()
}

val ASK_LOCATION_AREA_MESSAGE = """
    请输入你想要设置的地区
    
    亚洲
    北美
    巴西
    拉丁美洲
    韩国
    欧洲
    
    请输入正确的地区值
    """.trimIndent()


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