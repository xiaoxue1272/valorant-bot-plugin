package io.tiangou.logic


import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.*
import io.tiangou.api.RiotApi
import io.tiangou.api.data.AuthCookiesRequest
import io.tiangou.api.data.AuthRequest
import io.tiangou.api.data.AuthResponse
import io.tiangou.api.data.MultiFactorAuthRequest
import io.tiangou.logic.image.utils.GenerateImageType
import io.tiangou.logic.image.utils.ImageHelper
import io.tiangou.logic.image.utils.StoreApiHelper
import io.tiangou.other.http.actions
import io.tiangou.other.http.client
import io.tiangou.repository.UserCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.extension
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.ImageType
import net.mamoe.mirai.message.data.firstIsInstanceOrNull

@Serializable
sealed interface LogicProcessor<T : MessageEvent> {
    suspend fun process(event: MessageEvent, userCache: UserCache): Boolean

    fun synchronousProcess(event: MessageEvent, userCache: UserCache): Boolean =
        userCache.synchronous { process(event, userCache) }
}

@Serializable
object HelpListLogicProcessor : LogicProcessor<MessageEvent>,
    Storage<String> by StringStorage("help-list.txt", StoragePathEnum.CONFIG_PATH) {

    private val helpMessage: String = runBlocking { load() ?: store(HELP_LIST_MESSAGE) }

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply(helpMessage)
        return false
    }

}

@Serializable
object AskRiotUsernameLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply("请输入Riot账号,待机器人回复消息后撤回")
        return false
    }
}

@Serializable
object SaveRiotUsernameLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        userCache.logicTransferData.account = event.message.toText()
        return true
    }
}

@Serializable
object AskRiotPasswordLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply("请输入Riot密码,待机器人回复消息后撤回")
        return false
    }
}

@Serializable
object SaveRiotPasswordLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        userCache.logicTransferData.password = event.message.toText()
        return true
    }

}

@Serializable
object LoginRiotAccountLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        return userCache.riotClientData.actions {
            // 因为cookie问题 如果是重复登录的话 登录不上去 所以清空
            event.reply("请稍等,正在登录")
            cookies.clear()
            RiotApi.AuthCookies.execute(AuthCookiesRequest())
            val authResponse: AuthResponse =
                RiotApi.Auth.execute(
                    AuthRequest(
                        userCache.logicTransferData.account!!,
                        userCache.logicTransferData.password!!
                    )
                )
            when (authResponse.type) {
                AuthResponse.RESPONSE -> {
                    userCache.loginSuccessfulHandle(event, authResponse.response!!.parameters.uri)
                }

                AuthResponse.MULTI_FACTOR -> {
                    userCache.logicTransferData.needLoginVerify = true
                    event.reply("请输入二步验证码")
                    return@actions false
                }

                else -> event.reply("登录失败,请检查用户名和密码是否正确")
            }
            return@actions true
        }
    }
}

@Serializable
object VerifyRiotAccountLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        if (userCache.logicTransferData.needLoginVerify == true) {
            event.reply("请稍等,正在验证")
            userCache.riotClientData.actions {
                val authResponse = RiotApi.MultiFactorAuth.execute(MultiFactorAuthRequest(event.message.toText()))
                when (authResponse.type) {
                    AuthResponse.RESPONSE -> {
                        userCache.loginSuccessfulHandle(event, authResponse.response!!.parameters.uri)
                    }

                    else -> event.reply("登录失败,请重新登录")
                }
            }
        }
        return false
    }
}

@Serializable
object AskLocationAreaLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply(ASK_LOCATION_AREA_MESSAGE)
        return false
    }
}

@Serializable
object SaveLocationShardLogicProcessor : LogicProcessor<MessageEvent> {
    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        val result = event.message.toText()
        ServerLocationEnum.values().forEach {
            if (it.value == result) {
                userCache.riotClientData.shard = it.shard
                userCache.riotClientData.region = it.region
                event.reply("设置成功")
                ImageHelper.clean(userCache)
                StoreApiHelper.clean(userCache)
                return false
            }
        }
        throw ValorantRuntimeException("未找到输入的地区")
    }

}

@Serializable
object CheckRiotStatusAndSettingProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        if (!userCache.isRiotAccountLogin) {
            throw ValorantRuntimeException("请先登录Riot账号")
        }
        if (userCache.riotClientData.shard == null && userCache.riotClientData.region == null) {
            throw ValorantRuntimeException("请先设置地区")
        }
        return true
    }

}

@Serializable
object CheckIsBotFriendProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        if (event.bot.getFriend(event.sender.id) == null) {
            throw ValorantRuntimeException("请添加机器人为好友")
        }
        return true
    }

}

@Serializable
object QueryPlayerDailyStoreProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply("正在查询每日商店,请稍等")
        val skinsPanelLayoutImage = ImageHelper.get(userCache, GenerateImageType.SKINS_PANEL_LAYOUT)
        event.uploadImage(skinsPanelLayoutImage)
        return false
    }
}

@Serializable
object SubscribeTaskDailyStoreProcessor : LogicProcessor<MessageEvent> {
    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        userCache.apply {
            subscribeDailyStore = !subscribeDailyStore
            val status: String = if (subscribeDailyStore) "开启" else "关闭"
            event.reply("已将你的每日商店推送状态设置为:$status")
        }
        return false
    }
}

@Serializable
object TellCustomBackgroundCanUploadProcessor : LogicProcessor<MessageEvent> {
    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply("请上传背景图片(若要回复为默认背景,请发送\"恢复默认\")")
        return false
    }
}

@Serializable
object UploadCustomBackgroundProcessor : LogicProcessor<MessageEvent> {
    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        if (event.message.toText() == "恢复默认") {
            userCache.customBackgroundFile = null
            event.reply("已将背景图片恢复至默认")
        } else {
            val downloadUrl = event.run {
                message.firstIsInstanceOrNull<Image>()?.queryUrl()
                    ?: takeIf { it.subject is Group }?.message?.firstIsInstanceOrNull<FileMessage>()
                        ?.toAbsoluteFile(subject.cast())?.takeIf { ImageType.matchOrNull(it.extension) != null }
                        ?.getUrl()
            } ?: event.reply("无法解析图片,请确认图片后缀无误,推荐上传PNG或JPG格式的图片").let { return false }
            userCache.customBackgroundFile = ValorantBotPlugin.dataFolder.resolve("${event.sender.id}_background.bkg")
                .apply { writeBytes(client.get(downloadUrl).readBytes()) }
            event.reply("上传成功")
        }
        ImageHelper.clean(userCache)
        return false
    }
}

@Serializable
object QueryPlayerAccessoryStoreProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply("正在查询配件商店,请稍等")
        val accessoryStoreImage = ImageHelper.get(userCache, GenerateImageType.ACCESSORY_STORE)
        event.uploadImage(accessoryStoreImage)
        return false
    }
}

@Serializable
object AskDailyStorePushLocationProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply(
            """
            请输入你想要指定的推送地点
            
            this : 代表当前会话窗口
            [群号]: 推送到QQ群
            
        """.trimIndent()
        )
        return false
    }
}

@Serializable
object ParseDailyStorePushLocationProcessor : LogicProcessor<MessageEvent> {

    suspend fun MessageEvent.isVisitAllow(qq: Long): Boolean {
        if (bot.getGroup(qq) == null) {
            reply("无法设置群[$qq]为推送地点,Bot并未在指定群中")
            return false
        }
        when (VisitConfig.controlType) {
            VisitControlEnum.WHITE_LIST -> if (!VisitConfig.onGroups.contains(qq)) {
                reply("群[$qq]暂无访问权限")
                return false
            }

            VisitControlEnum.BLACK_LIST -> if (VisitConfig.onGroups.contains(qq)) {
                reply("群[$qq]暂无访问权限")
                return false
            }
        }
        return true
    }

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        val text = event.message.toText()
        if (text.uppercase() == "THIS") {
            val isLocationEnabled = !(userCache.dailyStorePushLocations[event.subject.id] ?: false)
            userCache.dailyStorePushLocations[event.subject.id] = isLocationEnabled
            event.reply("已将当前地点的推送状态设置为:${if (isLocationEnabled) "启用" else "停用"}")
        } else if ("\\d+".toRegex().matches(text) && event.isVisitAllow(text.toLong())) {
            val isLocationEnabled = !(userCache.dailyStorePushLocations[text.toLong()] ?: false)
            userCache.dailyStorePushLocations[text.toLong()] = isLocationEnabled
            event.reply("已将指定群[$text]的推送状态设置为:${if (isLocationEnabled) "启用" else "停用"}")
        } else {
            event.reply("输入不正确,无法解析为正确的推送地点")
        }
        return false
    }
}