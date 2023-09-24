package io.tiangou.logic


import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.*
import io.tiangou.api.RiotApi
import io.tiangou.api.StoreApiHelper
import io.tiangou.api.data.AuthCookiesRequest
import io.tiangou.api.data.AuthRequest
import io.tiangou.api.data.AuthResponse
import io.tiangou.api.data.MultiFactorAuthRequest
import io.tiangou.other.http.actions
import io.tiangou.other.http.client
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
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
    suspend fun MessageEvent.process(userCache: UserCache)

}

@Serializable
object HelpListLogicProcessor : LogicProcessor<MessageEvent>,
    Storage<String> by StringStorage("help-list.txt", StoragePathEnum.CONFIG_PATH) {

    private val helpMessage: String = runBlocking { load() ?: store(default_help_list) }

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply(helpMessage)
    }

}

@Serializable
object LoginRiotAccountLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        var currentEvent = this
        currentEvent reply "请输入Riot账号,待机器人回复消息后撤回"
        currentEvent = nextMessageEvent()
        val account = currentEvent.toText()
        currentEvent reply "请输入Riot密码,待机器人回复消息后撤回"
        currentEvent = nextMessageEvent()
        val password = currentEvent.toText()
        currentEvent reply "请稍等,正在登录"
        userCache.synchronous {
            riotClientData.actions {
                // 因为cookie问题 如果是重复登录的话 登录不上去 所以清空
                cookies.clear()
                RiotApi.AuthCookies.execute(AuthCookiesRequest())
                val authResponse: AuthResponse = RiotApi.Auth.execute(AuthRequest(account, password))
                when (authResponse.type) {
                    AuthResponse.TypeEnum.RESPONSE -> {
                        currentEvent.afterLogin(userCache, authResponse.response!!.parameters.uri)
                    }

                    AuthResponse.TypeEnum.MULTI_FACTOR -> {
                        currentEvent.verifyMultiAuthCode(userCache)
                    }

                    else -> currentEvent reply "登录失败,请检查用户名和密码是否正确"
                }
            }
        }
    }

    private suspend fun MessageEvent.verifyMultiAuthCode(userCache: UserCache) {
        reply("请输入二步验证码")
        val currentEvent = nextMessageEvent()
        val verifyCode = currentEvent.toText()
        currentEvent reply "请稍等,正在验证"
        val authResponse = RiotApi.MultiFactorAuth.execute(MultiFactorAuthRequest(verifyCode))
        when (authResponse.type) {
            AuthResponse.TypeEnum.RESPONSE -> {
                afterLogin(userCache, authResponse.response!!.parameters.uri)
            }

            else -> currentEvent reply "登录失败,请重新登录"
        }
    }

    private suspend fun MessageEvent.afterLogin(userCache: UserCache, authUrl: String) {
        userCache.apply {
            StoreApiHelper.clean(this)
//            ImageGenerator.clean(this)
            generateImages.clear()
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
    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply(ASK_LOCATION_AREA_MESSAGE)
        val currentEvent = nextMessageEvent()
        val result = currentEvent.toText()
        ServerLocationEnum.values().forEach {
            if (it.value == result) {
                userCache.synchronous {
                    riotClientData.shard = it.shard
                    riotClientData.region = it.region
                    currentEvent reply "设置成功"
//                    ImageGenerator.clean(userCache)
                    generateImages.clear()
                    StoreApiHelper.clean(userCache)
                }
                return
            }
        }
        currentEvent reply "未找到输入的地区"
    }

}

@Serializable
object CheckRiotStatusAndSettingProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        if (!userCache.isRiotAccountLogin) {
            throw ValorantPluginException("请先登录Riot账号")
        }
        if (userCache.riotClientData.shard == null && userCache.riotClientData.region == null) {
            throw ValorantPluginException("请先设置地区")
        }
    }

}

@Serializable
object CheckIsBotFriendProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        if (bot.getFriend(sender.id) == null) {
            throw ValorantPluginException("请添加机器人为好友")
        }
    }

}

@Serializable
object QueryPlayerDailyStoreProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("正在查询每日商店,请稍等")
        val skinsPanelLayoutImage = ImageGenerator.getCacheOrGenerate(
            userCache,
            GenerateImageType.SKINS_PANEL_LAYOUT,
        ) { storeImage(userCache, it) }
        replyImage(skinsPanelLayoutImage)
    }
}

@Serializable
object SubscribeTaskDailyStoreProcessor : LogicProcessor<MessageEvent> {
    override suspend fun MessageEvent.process(userCache: UserCache) {
        userCache.synchronous {
            subscribeDailyStore = !subscribeDailyStore
            val status: String = if (subscribeDailyStore) "开启" else "关闭"
            reply("已将你的每日商店推送状态设置为:$status")
        }
    }
}

@Serializable
object UploadCustomBackgroundProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("请上传背景图片(若要回复为默认背景,请发送\"恢复默认\")")
        nextMessageEvent().apply {
            if (toText() == "恢复默认") {
                userCache.synchronous {
                    customBackgroundFile?.delete()
                    customBackgroundFile = null
                    generateImages.clear()
                }
                reply("已将背景图片恢复至默认")
            } else {
                val downloadUrl = message.firstIsInstanceOrNull<Image>()?.queryUrl()
                    ?: takeIf { it.subject is Group }?.message?.firstIsInstanceOrNull<FileMessage>()
                        ?.toAbsoluteFile(subject.cast())?.takeIf { ImageType.matchOrNull(it.extension) != null }
                        ?.getUrl()
                    ?: reply("无法解析图片,请确认图片后缀无误,推荐上传PNG或JPG格式的图片").let { return }
                userCache.synchronous {
                    customBackgroundFile = ValorantBotPlugin.dataFolder.resolve("${sender.id}_background.bkg")
                        .apply { writeBytes(client.get(downloadUrl).readBytes()) }
                    generateImages.clear()
                }
                reply("上传成功")
            }
        }
    }

}

@Serializable
object QueryPlayerAccessoryStoreProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("正在查询配件商店,请稍等")
        val accessoryStoreImage = ImageGenerator.getCacheOrGenerate(
            userCache,
            GenerateImageType.ACCESSORY_STORE,
        ) { storeImage(userCache, it) }
        replyImage(accessoryStoreImage)
    }
}

@Serializable
object AddLocateToDailyStorePushLocatesProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("请输入群号")
        nextMessageEvent().apply {
            val groupId = "\\d+".toRegex().let {
                val text = toText()
                if (!it.matches(text)) {
                    reply("输入不正确,无法解析为正确的推送地点")
                    return
                }
                text.toLong()
            }
            if (!isVisitAllow(groupId)) return
            val isGroupNotExists = userCache.subscribePushLocates[groupId] == null
            if (isGroupNotExists) {
                if (bot.containsGroup(groupId)) {
                    userCache.subscribePushLocates[groupId] = UserCache.ContactEnum.GROUP
                } else {
                    reply("未找到群[$groupId],请检查Bot是否在指定的群中")
                    return
                }
            } else {
                userCache.subscribePushLocates.remove(groupId)
            }
            reply("已将指定群[${groupId}]的推送状态设置为:${if (isGroupNotExists) "启用" else "停用"}")
        }
    }
}

@Serializable
object AddCurrentLocateToDailyStorePushLocatesProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        val isCurrentLocateNotExists = userCache.subscribePushLocates[subject.id] == null
        if (isCurrentLocateNotExists && isVisitAllow()) {
            if (bot.containsGroup(subject.id)) {
                userCache.subscribePushLocates[subject.id] = UserCache.ContactEnum.GROUP
            } else if (bot.containsFriend(subject.id) || bot.getStranger(subject.id) != null) {
                userCache.subscribePushLocates[subject.id] = UserCache.ContactEnum.USER
            } else {
                reply("暂不支持当前地点")
                return
            }
        } else {
            userCache.subscribePushLocates.remove(subject.id)
        }
        reply("已将当前地点[${subject.id}]的推送状态设置为:${if (isCurrentLocateNotExists) "启用" else "停用"}")
    }
}