package io.tiangou.logic


import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.*
import io.tiangou.api.RiotApi
import io.tiangou.api.data.AuthCookiesRequest
import io.tiangou.api.data.AuthRequest
import io.tiangou.api.data.AuthResponse
import io.tiangou.api.data.MultiFactorAuthRequest
import io.tiangou.other.http.actions
import io.tiangou.other.http.client
import io.tiangou.other.image.GenerateStoreImageType
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserData
import io.tiangou.api.StoreApiHelper
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.extension
import net.mamoe.mirai.containsFriend
import net.mamoe.mirai.containsGroup
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.ImageType
import net.mamoe.mirai.message.data.firstIsInstanceOrNull

@Serializable
sealed interface LogicProcessor<T : MessageEvent> {
    suspend fun MessageEvent.process(userData: UserData)

}

@Serializable
object HelpListLogicProcessor : LogicProcessor<MessageEvent>,
    Storage<String> by StringStorage("help-list.txt", StoragePathEnum.CONFIG_PATH) {

    private val helpMessage: String = runBlocking { load() ?: store(HELP_LIST_MESSAGE) }

    override suspend fun MessageEvent.process(userData: UserData) {
        reply(helpMessage)
    }

}

@Serializable
object LoginRiotAccountLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        var currentEvent = this
        currentEvent.reply("请输入Riot账号,待机器人回复消息后撤回")
        currentEvent = nextMessageEvent()
        val account = currentEvent.toText()
        currentEvent.reply("请输入Riot密码,待机器人回复消息后撤回")
        currentEvent = nextMessageEvent()
        val password = currentEvent.toText()
        currentEvent.reply("请稍等,正在登录")
        userData.synchronous {
            riotClientData.actions {
                // 因为cookie问题 如果是重复登录的话 登录不上去 所以清空
                cookies.clear()
                RiotApi.AuthCookies.execute(AuthCookiesRequest())
                val authResponse: AuthResponse = RiotApi.Auth.execute(AuthRequest(account, password))
                when (authResponse.type) {
                    AuthResponse.TypeEnum.RESPONSE -> {
                        currentEvent.afterLogin(userData, authResponse.response!!.parameters.uri)
                    }

                    AuthResponse.TypeEnum.MULTI_FACTOR -> {
                        currentEvent.verifyMultiAuthCode(userData)
                    }

                    else -> currentEvent.reply("登录失败,请检查用户名和密码是否正确")
                }
            }
        }
    }

    private suspend fun MessageEvent.verifyMultiAuthCode(userData: UserData) {
        reply("请输入二步验证码")
        val currentEvent = nextMessageEvent()
        val verifyCode = currentEvent.toText()
        currentEvent.reply("请稍等,正在验证")
        val authResponse = RiotApi.MultiFactorAuth.execute(MultiFactorAuthRequest(verifyCode))
        when (authResponse.type) {
            AuthResponse.TypeEnum.RESPONSE -> {
                afterLogin(userData, authResponse.response!!.parameters.uri)
            }

            else -> currentEvent.reply("登录失败,请重新登录")
        }
    }

    private suspend fun MessageEvent.afterLogin(userData: UserData, authUrl: String) {
        userData.apply {
            StoreApiHelper.clean(this)
            ImageGenerator.clean(this)
            riotClientData.flushAccessToken(authUrl)
            riotClientData.flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            riotClientData.puuid = RiotApi.PlayerInfo.execute().sub
            isRiotAccountLogin = true
        }
        reply("登录成功")
    }
}

@Serializable
object ChangeLocationShardLogicProcessor : LogicProcessor<MessageEvent> {
    override suspend fun MessageEvent.process(userData: UserData) {
        reply(ASK_LOCATION_AREA_MESSAGE)
        val currentEvent = nextMessageEvent()
        val result = currentEvent.toText()
        ServerLocationEnum.values().forEach {
            if (it.value == result) {
                userData.synchronous {
                    riotClientData.shard = it.shard
                    riotClientData.region = it.region
                    currentEvent.reply("设置成功")
                    ImageGenerator.clean(userData)
                    StoreApiHelper.clean(userData)
                }
                return
            }
        }
        currentEvent.reply("未找到输入的地区")
    }

}

@Serializable
object CheckRiotStatusAndSettingProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        if (!userData.isRiotAccountLogin) {
            throw ValorantRuntimeException("请先登录Riot账号")
        }
        if (userData.riotClientData.shard == null && userData.riotClientData.region == null) {
            throw ValorantRuntimeException("请先设置地区")
        }
    }

}

@Serializable
object CheckIsBotFriendProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        if (bot.getFriend(sender.id) == null) {
            throw ValorantRuntimeException("请添加机器人为好友")
        }
    }

}

@Serializable
object QueryPlayerDailyStoreProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        reply("正在查询每日商店,请稍等")
        val skinsPanelLayoutImage = ImageGenerator.getCacheOrGenerate(
            userData,
            GenerateStoreImageType.SKINS_PANEL_LAYOUT,
        ) { storeImage(userData, it) }
        replyImage(skinsPanelLayoutImage)
    }
}

@Serializable
object SubscribeTaskDailyStoreProcessor : LogicProcessor<MessageEvent> {
    override suspend fun MessageEvent.process(userData: UserData) {
        userData.synchronous {
            subscribeDailyStore = !subscribeDailyStore
            val status: String = if (subscribeDailyStore) "开启" else "关闭"
            reply("已将你的每日商店推送状态设置为:$status")
        }
    }
}

@Serializable
object UploadCustomBackgroundProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        reply("请上传背景图片(若要回复为默认背景,请发送\"恢复默认\")")
        nextMessageEvent().apply {
            if (toText() == "恢复默认") {
                userData.synchronous {
                    customBackgroundFile?.delete()
                    customBackgroundFile = null
                }
                reply("已将背景图片恢复至默认")
            } else {
                val downloadUrl = message.firstIsInstanceOrNull<Image>()?.queryUrl()
                    ?: takeIf { it.subject is Group }?.message?.firstIsInstanceOrNull<FileMessage>()
                        ?.toAbsoluteFile(subject.cast())?.takeIf { ImageType.matchOrNull(it.extension) != null }
                        ?.getUrl()
                    ?: reply("无法解析图片,请确认图片后缀无误,推荐上传PNG或JPG格式的图片").let { return }
                userData.synchronous {
                    customBackgroundFile = ValorantBotPlugin.dataFolder.resolve("${sender.id}_background.bkg")
                        .apply { writeBytes(client.get(downloadUrl).readBytes()) }
                }
                ImageGenerator.clean(userData)
                reply("上传成功")
            }
        }
    }

}

@Serializable
object QueryPlayerAccessoryStoreProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        reply("正在查询配件商店,请稍等")
        val accessoryStoreImage = ImageGenerator.getCacheOrGenerate(
            userData,
            GenerateStoreImageType.ACCESSORY_STORE,
        ) { storeImage(userData, it) }
        replyImage(accessoryStoreImage)
    }
}

@Serializable
object AddLocateToDailyStorePushLocatesProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        reply("请输入群号")
        nextMessageEvent().apply {
            val text = toText()
            if ("\\d+".toRegex().matches(text) && isVisitAllow(text)) {
                val groupId = text.toLong()
                val isEnabledLocate = userData.dailyStorePushLocates[groupId] == null
                if (isEnabledLocate) {
                    if (bot.containsGroup(groupId)) {
                        userData.dailyStorePushLocates[groupId] = UserData.ContactEnum.GROUP
                    } else {
                        reply("未找到群[$text],请检查Bot是否在指定的群中")
                        return
                    }
                } else {
                    userData.dailyStorePushLocates.remove(groupId)
                }
                reply("已将指定群[${text}]的推送状态设置为:${if (isEnabledLocate) "启用" else "停用"}")
            } else {
                reply("输入不正确,无法解析为正确的推送地点")
            }
        }
    }
}

@Serializable
object AddCurrentLocateToDailyStorePushLocatesProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userData: UserData) {
        val isEnabledLocate = userData.dailyStorePushLocates[subject.id] == null
        if (isEnabledLocate) {
            if (bot.containsGroup(subject.id)) {
                userData.dailyStorePushLocates[subject.id] = UserData.ContactEnum.GROUP
            } else if (bot.containsFriend(subject.id) || bot.getStranger(subject.id) != null) {
                userData.dailyStorePushLocates[subject.id] = UserData.ContactEnum.USER
            } else {
                reply("暂不支持当前地点")
                return
            }
        } else {
            userData.dailyStorePushLocates.remove(subject.id)
        }
        reply("已将当前地点[${subject.id}]的推送状态设置为:${if (isEnabledLocate) "启用" else "停用"}")
    }
}