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


// todo 再写一个每周配件商店订阅任务 公共推送数据结构部分抽出来
@Serializable
class SubscribeDailyStoreCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : CronTask() {


    override val description: String = "每日商店推送"

    data class UserSubscribeLocation(val user: User, val contacts: List<Contact>)

    override suspend fun execute() {
        val currentDateTime = now(timeZone.toZoneId()).format()
        UserCacheRepository.getAllUserCache()
            .filterValues {
                it.isRiotAccountLogin
            }.forEach { (userQQ, userCache) ->
                val userSubscribePushDataList = userCache.mapSubscribeByBot(userQQ, getOnlineBots())
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


    private fun UserCache.mapSubscribeByBot(userQQ: Long, onlineBots: List<Bot>): List<UserSubscribeLocation> {
        val contactQQSet = subscribes
            .filterValues { it.contains(SubscribeType.DAILY_STORE) }
            .keys
            .toMutableSet()
        val result = mutableListOf<UserSubscribeLocation>()
        onlineBots.forEach { bot ->
            val user = bot.getUser(userQQ)
            if (user != null) {
                val contactList = mutableListOf<Contact>()
                for (contactQQ in contactQQSet.toList()) {
                    val contact = bot.getContact(contactQQ)
                    if (contact != null) {
                        contactList.add(contact)
                        contactQQSet.remove(contactQQ)
                    }
                }
                if (contactList.isNotEmpty()) result.add(UserSubscribeLocation(user, contactList))
            }
        }
        return result
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