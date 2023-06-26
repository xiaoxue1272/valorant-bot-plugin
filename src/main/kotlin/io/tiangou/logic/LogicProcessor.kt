package io.tiangou.logic


import io.tiangou.*
import io.tiangou.EventHandler.getContact
import io.tiangou.EventHandler.reply
import io.tiangou.api.RiotApi
import io.tiangou.api.data.AuthCookiesRequest
import io.tiangou.api.data.AuthRequest
import io.tiangou.api.data.AuthResponse
import io.tiangou.api.data.MultiFactorAuthRequest
import io.tiangou.logic.utils.DailyStoreImageGenerator
import io.tiangou.other.http.actions
import io.tiangou.repository.UserCache
import io.tiangou.repository.getAndClear
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image.Key.isUploaded
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.lang.ref.WeakReference

@Serializable
sealed interface LogicProcessor<T : MessageEvent> {
    suspend fun process(event: MessageEvent, userCache: UserCache): Boolean

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
        userCache.logicTransferData.account = WeakReference(event.message.toText())
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
        userCache.logicTransferData.password = WeakReference(event.message.toText())
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
                        userCache.logicTransferData.account!!.getAndClear(),
                        userCache.logicTransferData.password!!.getAndClear()
                    )
                )
            when (authResponse.type) {
                AuthResponse.RESPONSE -> {
                    flushAccessToken(authResponse.response!!.parameters.uri)
                    flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
                    event.reply("登录成功")
                    userCache.isRiotAccountLogin = true
                }

                AuthResponse.MULTI_FACTOR -> {
                    userCache.logicTransferData.needLoginVerify = WeakReference(true)
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
        if (userCache.logicTransferData.needLoginVerify?.getAndClear() == true) {
            event.reply("请稍等,正在验证")
            userCache.riotClientData.actions {
                val authResponse = RiotApi.MultiFactorAuth.execute(MultiFactorAuthRequest(event.message.toText()))
                when (authResponse.type) {
                    AuthResponse.RESPONSE -> {
                        flushAccessToken(authResponse.response!!.parameters.uri)
                        flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
                        event.reply("登录成功")
                        userCache.isRiotAccountLogin = true
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
        event.reply(
            MessageChainBuilder()
                .append("请输入你想要设置的地区\n")
                .append("\n")
                .append("亚洲\n")
                .append("北美\n")
                .append("巴西\n")
                .append("拉丁美洲\n")
                .append("韩国\n")
                .append("欧洲\n")
                .append("\n")
                .append("请输入正确的地区值")
                .build()
        )
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
                return false
            }
        }
        throw ValorantRuntimeException("未找到输入的地区")
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
        EU("欧洲", "eu", "eu"),
        ;
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
object QueryPlayerDailyStoreItemProcessor : LogicProcessor<MessageEvent> {

    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        event.reply("正在查询每日商店,请稍等")
        userCache.riotClientData.actions {
            val dailyStore = DailyStoreImageGenerator.generate(this)
            var uploadImage =
                event.getContact().uploadImage(dailyStore.toExternalResource().toAutoCloseable())
            repeat(2) {
                if (!uploadImage.isUploaded(event.bot)) {
                    uploadImage = event.getContact()
                        .uploadImage(dailyStore.toExternalResource().toAutoCloseable())
                }
            }
            if (uploadImage.isUploaded(event.bot)) {
                event.reply(MessageChainBuilder().append(uploadImage).build())
            } else {
                event.reply("图片上传失败,请稍候重试")
            }
        }
        return false
    }
}



@Serializable
object SubscribeTaskDailyStore : LogicProcessor<MessageEvent> {
    override suspend fun process(event: MessageEvent, userCache: UserCache): Boolean {
        userCache.apply {
            subscribeDailyStore = !subscribeDailyStore
            val status: String = if (subscribeDailyStore) "开启" else "关闭"
            event.reply("已将你的每日商店推送状态设置为:$status")
        }
        return false
    }
}