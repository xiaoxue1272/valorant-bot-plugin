package io.tiangou.cron

import io.tiangou.config.PluginConfig
import io.tiangou.getOnlineBots
import io.tiangou.getUser
import io.tiangou.reply
import io.tiangou.repository.UserCacheRepository
import kotlinx.serialization.Serializable

@Serializable
class RiotAccountLoginExpiredCronTask(
    override var isEnable: Boolean,
) : CronTask() {

    override var cron: String = "0 0 12 * * ? *"
        @Suppress("unused") set(value) = throw IllegalArgumentException("此任务cron表达式不允许修改")

    override val description: String = "Riot账号登录过期处理"

    override suspend fun execute() {
        val onlineBots = getOnlineBots()
        UserCacheRepository.getAllUserCache()
            .filterValues { !it.isRiotAccountLogin }
            .forEach { (userQQ, userCache) ->
                userCache.synchronous {
                    logoutDay++
                    if (logoutDay > PluginConfig.logoutRiotAccountCleanDay - 1) {
                        val user = onlineBots.firstNotNullOfOrNull { it.getUser(userQQ) }
                        if (logoutDay == PluginConfig.logoutRiotAccountCleanDay) {
                            riotClientData.clean()
                            user?.reply("当前Riot登录过期已达7天,请重新登录,若超过30天则会清理所有账号信息,包括自定义背景")
                        }
                        if (logoutDay == PluginConfig.logoutUserCacheCleanDay) {
                            UserCacheRepository
                            user?.reply("当前Riot登录过期已达30天,已清理所有数据")
                        }
                    }
                }
            }
    }
}