package io.tiangou.cron

import io.tiangou.*
import io.tiangou.api.RiotApiHelper
import io.tiangou.delay.UserImageCacheCleanTask
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
import io.tiangou.repository.UserCacheRepository
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChainBuilder
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class DailyStorePushCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : CronTask() {


    override val description: String = "每日商店推送"

    data class UserSubscribePushData(val user: User, val contacts: List<Contact>)

    override suspend fun execute() {
        val currentDateTime = now(timeZone.toZoneId()).format()
        UserCacheRepository.getAllUserCache()
            .filterValues {
                it.isRiotAccountLogin
            }.forEach { (userQQ, userCache) ->
                val userSubscribePushDataList = userCache.mapSubscribeDataByBots(userQQ, getOnlineBots())
                userSubscribePushDataList
                    .takeIf { it.isNotEmpty() }
                    ?.forEach { userSubscribePushData ->
                        runCatching {
                            userSubscribePushData.contacts.forEach { contact ->
                                val skinsPanelLayoutImage =
                                    userCache.getOrCacheImage(GenerateImageType.SKINS_PANEL_LAYOUT) {
                                        val storeFront = RiotApiHelper.queryStoreFrontApi(userCache)
                                        UserImageCacheCleanTask(
                                            storeFront.skinsPanelLayout.singleItemOffersRemainingDurationInSeconds.toDuration(
                                                DurationUnit.SECONDS
                                            ),
                                            it,
                                            userQQ
                                        ).enable()
                                        ImageGenerator.storeImage(
                                            userCache,
                                            GenerateImageType.SKINS_PANEL_LAYOUT,
                                            storeFront
                                        )
                                    }.getOrFail()
                                val image = uploadImageOrFail(skinsPanelLayoutImage, contact)
                                val message =
                                    MessageChainBuilder().append("$currentDateTime\n 今日商店为:\n").append(image)
                                        .build()
                                contact.reply(message, userQQ)
                            }
                        }.onFailure { throwable ->
                            log.warning("QQ:[${userQQ}],推送每日商店时异常,异常信息:", throwable)
                            userSubscribePushData.user.sendMessage("推送每日商店时出错,错误信息:[$throwable]")
                        }
                    }
            }
    }


    private fun UserCache.mapSubscribeDataByBots(userQQ: Long, onlineBots: List<Bot>): List<UserSubscribePushData> {
        val subscribeLocates = subscribes
            .filterValues { it.contains(SubscribeType.DAILY_STORE) }
            .keys
            .toHashSet()
        return onlineBots.mapNotNull {
            val user = it.getUser(userQQ) ?: return@mapNotNull null
            val list = mutableListOf<Contact>()
            subscribeLocates.iterator().forEach { contactQQ ->
                if (isVisitAllow(contactQQ, it)) {
                    val contact = it.getContact(contactQQ)
                    if (contact != null) {
                        list.add(contact)
                        subscribeLocates.remove(contactQQ)
                    }
                }
            }
            if (list.isEmpty()) return@mapNotNull null
            UserSubscribePushData(user, list)
        }
    }

}

//    private infix fun MutableMap<Long, UserCache.SubscribeData>.mapToContactByBot(onlineBots: List<Bot>): List<Contact> {
//        val resultContactList = arrayListOf<Contact>()
//        val copyContactMap = this.toMutableMap()
//        for (bot in onlineBots) {
//            val filteredContactList = copyContactMap.filterKeys {
//                isVisitAllow(it, bot)
//            }.mapNotNull { location ->
//                val (qq, subscribeData) = location
//                val contact = when (subscribeData.contactEnum) {
//                    UserCache.ContactEnum.GROUP -> bot.getGroup(qq)?.takeIf { it.members.contains(qq) }
//                    UserCache.ContactEnum.USER -> bot.getUser(qq)
//                }
//                if (contact != null) copyContactMap.remove(qq)
//                return@mapNotNull contact
//            }
//            resultContactList.addAll(filteredContactList)
//        }
//        return resultContactList
//    }
//