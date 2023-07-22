package io.tiangou.cron

import io.tiangou.isVisitAllow
import io.tiangou.logic.image.utils.GenerateImageType
import io.tiangou.logic.image.utils.ImageHelper
import io.tiangou.reply
import io.tiangou.repository.UserCache
import io.tiangou.repository.UserCacheRepository
import io.tiangou.uploadImage
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
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
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now(timeZone.toZoneId()))
        val onlineBots = Bot.instances.filter { it.isOnline }
        for (entry in UserCacheRepository.getAllUserCache()) {
            val userCache = entry.value
            if (userCache.subscribeDailyStore) {
                val locations = userCache.filterPushLocations(entry.key, onlineBots)
                locations.takeIf { it.isNotEmpty() }
                    ?.runCatching {
                        val skinsPanelLayoutImage =
                            ImageHelper.get(userCache, GenerateImageType.SKINS_PANEL_LAYOUT)
                        locations.forEach { location ->
                            location.contacts.forEach { contact ->
                                uploadImage(skinsPanelLayoutImage, contact, location.bot)?.apply {
                                    contact.reply(
                                        MessageChainBuilder()
                                            .append("$date\n 今日商店为:\n")
                                            .append(this)
                                            .build(),
                                        location.user
                                    )
                                }
                            }
                        }
                    }?.onFailure { throwable ->
                        log.warning("QQ:[${entry.key}],推送每日商店时异常,异常信息:", throwable)
                        locations.first().user.sendMessage("推送每日商店时出错,错误信息:[$throwable]")
                    }
            }
        }
    }

    private fun UserCache.filterPushLocations(userQQ: Long, onlineBots: List<Bot>): List<BotPushLocation> {
        return mutableListOf<BotPushLocation>().apply {
            for (bot in onlineBots) {
                val user = bot.getFriend(userQQ) ?: bot.getStranger(userQQ)
                if (user != null) {
                    dailyStorePushLocations.filter { it.value }.mapNotNull { location ->
                        (bot.getGroup(location.key) ?: bot.getFriend(location.key) ?: bot.getStranger(location.key))
                            ?.takeIf { isVisitAllow(it.id, bot) }
                    }.takeIf { it.isNotEmpty() }?.let {
                        add(BotPushLocation(bot, user, it))
                    }
                }
            }
        }
    }

    data class BotPushLocation(val bot: Bot, val user: User, val contacts: List<Contact>)

}