package io.tiangou.cron

import io.tiangou.getOnlineBots
import io.tiangou.getUser
import io.tiangou.reply
import io.tiangou.repository.UserCacheRepository

class RiotAccountLoginExpiredTask(
    override var cron: String,
    override var isEnable: Boolean,
) : Task() {

    override val description: String = "Riot账号登录过期处理"

    override suspend fun execute() {
        UserCacheRepository.getAllUserCache()
            .filter { !it.value.isRiotAccountLogin }
            .forEach { entry ->
                entry.value.synchronous {
                    riotClientData.clean()
                    getOnlineBots()
                        .forEach {
                            it.getUser(entry.key)
                                ?.apply {
                                    reply("当前Riot登录已过期,请重新登录,若不希望收到此提示,请执行[]以清除所有用户信息")
                                    return@synchronous
                                }
                        }
                }
            }
    }
}