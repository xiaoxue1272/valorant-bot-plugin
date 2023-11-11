package io.tiangou.cron

import io.tiangou.api.ApiException
import io.tiangou.api.RiotApi
import io.tiangou.config.PluginConfig
import io.tiangou.getOnlineBots
import io.tiangou.getUser
import io.tiangou.other.http.actions
import io.tiangou.reply
import io.tiangou.repository.UserCacheRepository
import kotlinx.serialization.Serializable

@Serializable
class RiotAccountSecurityDataFlushCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : CronTask() {

    override val description: String = "Riot账号安全令牌刷新"

    override suspend fun execute() {
        UserCacheRepository.getAllUserCache().forEach { (qq, userCache) ->
            userCache.takeIf { it.isRiotAccountLogin }?.synchronous {
                riotClientData.actions {
                    runCatching {
                        flushAccessToken(RiotApi.CookieReAuth.execute())
                        flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
                    }.onFailure {
                        if (it is ApiException) {
                            isRiotAccountLogin = false
                            if (PluginConfig.enableSecurityTokenExpiredRemind) {
                                getOnlineBots().firstNotNullOfOrNull { bot -> bot.getUser(qq) }?.reply("Riot账号安全令牌刷新失败:[${it.message}]")
                            }
                        }
                        log.warning("Valorant安全令牌刷新任务,QQ:[${qq}],异常信息:", it)
                    }
                }
            }
        }
    }

}