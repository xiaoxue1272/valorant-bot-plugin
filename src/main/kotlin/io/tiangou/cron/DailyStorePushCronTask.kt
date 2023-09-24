package io.tiangou.cron

import io.tiangou.*
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
import io.tiangou.repository.UserCacheRepository
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
class DailyStorePushCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : CronTask() {


    override val description: String = "每日商店推送"

    override suspend fun execute() {
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now(timeZone.toZoneId()))
        for (entry in UserCacheRepository.getAllUserCache()) {
            val userCache = entry.value
            if (userCache.subscribeDailyStore) {
                val locations = userCache.filterPushLocations(entry.key, getOnlineBots())
                locations.takeIf { it.isNotEmpty() }
                    ?.runCatching {
                        val skinsPanelLayoutImage = ImageGenerator.getCacheOrGenerate(
                            userCache,
                            GenerateImageType.SKINS_PANEL_LAYOUT,
                        ) { storeImage(userCache, it) }
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

    private fun UserCache.filterPushLocations(qq: Long, onlineBots: List<Bot>): List<BotPushLocation> {
        val currentPushLocates = subscribePushLocates.toMutableMap()
        return mutableListOf<BotPushLocation>().apply {
            for (bot in onlineBots) {
                val user = bot.getUser(qq)
                if (user != null) {
                    currentPushLocates.toMutableMap().mapNotNull { location ->
                        when (location.value) {
                            UserCache.ContactEnum.GROUP -> bot.getGroup(location.key)
                            UserCache.ContactEnum.USER -> bot.getUser(qq)
                        }?.takeIf {
                            isVisitAllow(it.id, bot)
                        }?.apply {
                            currentPushLocates.remove(location.key)
                        }
                    }.takeIf { it.isNotEmpty() }?.let {
                        add(BotPushLocation(bot, user, it))
                    }
                }
            }
        }
    }

    data class BotPushLocation(val bot: Bot, val user: User, val contacts: List<Contact>)

}