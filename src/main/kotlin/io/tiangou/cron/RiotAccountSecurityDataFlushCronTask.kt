package io.tiangou.cron

import io.tiangou.api.ApiException
import io.tiangou.api.RiotApi
import io.tiangou.other.http.actions
import io.tiangou.repository.UserCacheRepository
import kotlinx.serialization.Serializable

@Serializable
class RiotAccountSecurityDataFlushCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : CronTask() {

    override val description: String = "Riot账号安全令牌刷新"

    override suspend fun execute() {
        UserCacheRepository.getAllUserCache().forEach { entry ->
            entry.value.takeIf { it.isRiotAccountLogin }?.synchronous {
                riotClientData.actions {
                    runCatching {
                        flushAccessToken(RiotApi.CookieReAuth.execute())
                        flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
                    }.onFailure {
                        if (it is ApiException) isRiotAccountLogin = false
                        log.warning("Valorant安全令牌刷新任务,QQ:[${entry.key}],异常信息:", it)
                    }
                }
            }
        }
    }

}