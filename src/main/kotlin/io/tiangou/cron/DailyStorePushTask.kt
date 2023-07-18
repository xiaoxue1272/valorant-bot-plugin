package io.tiangou.cron

import io.tiangou.logic.utils.GenerateImageType
import io.tiangou.logic.utils.StoreImageHelper
import io.tiangou.repository.UserCacheRepository
import io.tiangou.uploadImage
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
class DailyStorePushTask(
    override var cron: String,
    override var isEnable: Boolean
) : Task() {


    override val description: String = "每日商店推送"

    override suspend fun execute() {
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
        val onlineBots = Bot.instances.filter { it.isOnline }
        for (entry in UserCacheRepository.getAllUserCache()) {
            if (entry.value.subscribeDailyStore) {
                val (bot, user) = onlineBots.findBotAsUserFriend(entry.key) ?: continue
                runCatching {
                    val skinsPanelLayoutImage = StoreImageHelper.get(entry.value, GenerateImageType.SKINS_PANEL_LAYOUT)
                    uploadImage(skinsPanelLayoutImage, user, bot)?.apply {
                        user.sendMessage(
                            MessageChainBuilder()
                                .append("$date 今日商店为:\n")
                                .append(this)
                                .build()
                        )
                    }
                }.onFailure {
                    log.warning("QQ:[${entry.key}],推送每日商店内容异常,异常信息:", it)
                    user.sendMessage("推送每日商店失败,失败信息:[$it]")
                }
            }
        }
    }

    private fun List<Bot>.findBotAsUserFriend(qq: Long): BotUserRelation? {
        for (bot in this) {
            val user = bot.getFriend(qq) ?: bot.getStranger(qq)
            if (user != null) {
                return BotUserRelation(bot, user)
            }
        }
        log.info("QQ:[$qq],未在机器人联系列表中找到用户,请添加机器人为好友")
        return null
    }

    data class BotUserRelation(val bot: Bot, val user: User)

}