package io.tiangou.logic


import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.*
import io.tiangou.api.RiotApi
import io.tiangou.api.RiotApiHelper
import io.tiangou.api.data.AuthCookiesRequest
import io.tiangou.api.data.AuthRequest
import io.tiangou.api.data.AuthResponse
import io.tiangou.api.data.MultiFactorAuthRequest
import io.tiangou.delay.UserImageCacheCleanTask
import io.tiangou.other.http.actions
import io.tiangou.other.http.client
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.extension
import net.mamoe.mirai.containsGroup
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
//            RiotApiHelper.clean(this)
//            ImageGenerator.clean(this)
            logoutDay = 0
            cleanCacheImages()
            riotClientData.flushAccessToken(authUrl)
            riotClientData.flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            riotClientData.puuid = RiotApi.PlayerInfo.execute().sub
            isRiotAccountLogin = true
        }
        reply("登录成功")
    }
}

@Serializable
object LogoutRiotAccountLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        userCache.isRiotAccountLogin = false
        userCache.riotClientData.clean()
        userCache.cleanCacheImages()
        reply("已退出登录")
    }

}

@Serializable
object DeleteUserCacheLogicProcessor: LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        userCache.customBackgroundFile?.delete()
        userCache.cleanCacheImages()
        UserCacheRepository.remove(sender.id)
        reply("已删除用户数据")
    }

}

@Serializable
object ChangeLocationShardLogicProcessor : LogicProcessor<MessageEvent> {
    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply(askEnumMessage<ServerLocationEnum>())
        val currentEvent = nextMessageEvent()
        userCache.synchronous {
            ServerLocationEnum.findNotNull(currentEvent.toText()).apply {
                riotClientData.shard = shard
                riotClientData.region = region
                currentEvent reply "设置成功"
                cleanCacheImages()
            }
        }
    }

}

@Serializable
object CheckRiotStatusLogicProcessor : LogicProcessor<MessageEvent> {

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
object QueryDailyStoreLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("正在查询每日商店,请稍等")
        val skinsPanelLayoutImage = userCache.getOrCacheImage(GenerateImageType.SKINS_PANEL_LAYOUT) {
            val storeFront = RiotApiHelper.queryStoreFrontApi(userCache)
            UserImageCacheCleanTask(
                storeFront.skinsPanelLayout.singleItemOffersRemainingDurationInSeconds.toDuration(DurationUnit.SECONDS),
                it,
                sender.id
            ).enable()
            ImageGenerator.storeImage(userCache, it, storeFront)
        }.getOrFail()
        replyImage(skinsPanelLayoutImage)

    }
}

//@Serializable
//object SubscribeTaskDailyStoreProcessor : LogicProcessor<MessageEvent> {
//    override suspend fun MessageEvent.process(userCache: UserCache) {
//        userCache.synchronous {
//            val dailyStore = UserCache.SubscribeType.DAILY_STORE
//            val status: String =
//                if (subscribeTypeList.remove(dailyStore)) "关闭"
//                else subscribeTypeList.add(dailyStore).let { "开启" }
//            reply("已将你的每日商店推送状态设置为:$status")
//        }
//    }
//}

@Serializable
object UploadCustomBackgroundLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("请上传背景图片(若要回复为默认背景,请发送\"恢复默认\")")
        nextMessageEvent().apply {
            if (toText() == "恢复默认") {
                userCache.synchronous {
                    customBackgroundFile?.delete()
                    customBackgroundFile = null
                    cleanCacheImages()
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
                    cleanCacheImages()
                }
                reply("上传成功")
            }
        }
    }

}

@Serializable
object QueryAccessoryStoreLogicProcessor : LogicProcessor<MessageEvent> {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("正在查询配件商店,请稍等")
        val accessoryStoreImage = userCache.getOrCacheImage(GenerateImageType.ACCESSORY_STORE) {
            val storeFront = RiotApiHelper.queryStoreFrontApi(userCache)
            UserImageCacheCleanTask(
                storeFront.accessoryStore.accessoryStoreRemainingDurationInSeconds.toDuration(DurationUnit.SECONDS),
                it,
                sender.id
            ).enable()
            ImageGenerator.storeImage(userCache, it, storeFront)
        }.getOrFail()
        replyImage(accessoryStoreImage)
    }
}

@Serializable
sealed class AbstractSubscribeSettingLogicProcessor : LogicProcessor<MessageEvent> {

    data class SubscribeInfo(
        val currentMessageEvent: MessageEvent,
        val subscribeType: SubscribeType
    )

    protected suspend fun MessageEvent.readInputSubscribe(): SubscribeInfo {
        reply(askEnumMessage<SubscribeType>())
        val currentMessageEvent = nextMessageEvent()
        val subscribeType = SubscribeType.findNotNull(currentMessageEvent.toText())
        return SubscribeInfo(currentMessageEvent, subscribeType)
    }

}

@Serializable
object DesignateSubscribeSettingLogicProcessor : AbstractSubscribeSettingLogicProcessor() {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        val subscribeInfo = readInputSubscribe()
        val subscribeType = subscribeInfo.subscribeType
        var currentMessageEvent = subscribeInfo.currentMessageEvent
        currentMessageEvent reply "请输入群号"
        currentMessageEvent = nextMessageEvent()
        val groupId = "\\d+".toRegex().let {
            val text = currentMessageEvent.toText()
            if (!it.matches(text)) {
                currentMessageEvent reply "输入不正确,无法解析为正确的推送地点"
                return
            }
            text.toLong()
        }
        if (!isVisitAllow(groupId)) return
        val isDesignateSubscribeNotExists = userCache.subscribes[groupId]?.contains(subscribeType) != true
        if (isDesignateSubscribeNotExists) {
            if (bot.containsGroup(groupId)) {
                userCache.subscribes.getOrPut(groupId) { mutableListOf() }.add(subscribeType)
            } else {
                currentMessageEvent reply "未找到群[$groupId],当前机器人可能不在指定的群中"
                return
            }
        } else {
            userCache.subscribes.getOrPut(groupId) { mutableListOf() }.remove(subscribeType)
        }
        currentMessageEvent reply "已将指定群[${groupId}]的[${subscribeType.value}]的推送状态设置为:${if (isDesignateSubscribeNotExists) "启用" else "停用"}"
    }
}

@Serializable
object CurrentSubscribeSettingLogicProcessor : AbstractSubscribeSettingLogicProcessor() {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        if (!isVisitAllow()) return
        val (currentMessageEvent, subscribeType) = readInputSubscribe()
        val isCurrentSubscribeNotExists = userCache.subscribes[subject.id]?.contains(subscribeType) != true
        if (isCurrentSubscribeNotExists) {
            if (bot.containsGroup(subject.id)) {
                userCache.subscribes.getOrPut(subject.id) { mutableListOf() }.add(subscribeType)
            } else if (bot.getUser(subject.id) != null) {
                userCache.subscribes.getOrPut(subject.id) { mutableListOf() }.add(subscribeType)
            } else {
                currentMessageEvent reply "暂不支持设置当前地点"
                return
            }
        } else {
            userCache.subscribes.remove(subject.id)
        }
        currentMessageEvent reply "已将当前地点[${subject.id}]的[${subscribeType.value}]的推送状态设置为:${if (isCurrentSubscribeNotExists) "启用" else "停用"}"
    }
}

@Serializable
sealed class AbstractViewSubscribeSettingLogicProcessor: LogicProcessor<MessageEvent> {

    protected fun MessageEvent.getSubscribes(userCache: UserCache, contactQQ: Long? = null) =
        userCache.subscribes
            .run { if (contactQQ != null) filter { it.key == contactQQ } else this }
            .mapValues { it.value.map { type -> type.value } }
            .toList()
            .joinToString("\n\n") { "${bot.getContactType(it.first)}:[${it.first}]\n订阅类型:[${it.second.joinToString(", ")}]" }
            .takeIf { it.isNotEmpty() } ?: "暂无推送设置"
    
}

@Serializable
object ViewDesignateSubscribeSettingLogicProcessor: AbstractViewSubscribeSettingLogicProcessor() {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply("请输入群号")
        val currentMessageEvent = nextMessageEvent()
        val groupId = "\\d+".toRegex().let {
            val text = currentMessageEvent.toText()
            if (!it.matches(text)) {
                currentMessageEvent reply "输入不正确,无法解析为正确的推送地点"
                return
            }
            text.toLong()
        }
        currentMessageEvent reply currentMessageEvent.getSubscribes(userCache, groupId)
    }

}

@Serializable
object ViewCurrentSubscribeSettingLogicProcessor: AbstractViewSubscribeSettingLogicProcessor() {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply(getSubscribes(userCache, subject.id))
    }

}

@Serializable
object ViewAllSubscribeSettingLogicProcessor: AbstractViewSubscribeSettingLogicProcessor() {

    override suspend fun MessageEvent.process(userCache: UserCache) {
        reply(getSubscribes(userCache))
    }

}