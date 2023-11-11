package io.tiangou.cron

import io.tiangou.*
import io.tiangou.api.RiotApiHelper
import io.tiangou.delay.UserImageCacheCleanTask
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.DurationUnit
import kotlin.time.toDuration


// todo 再写一个每周配件商店订阅任务 公共推送数据结构部分抽出来
@Serializable
class SubscribeDailyStoreCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : AbstractSubscribeCronTask() {


    @Transient
    override val description: String = "每日商店推送"

    @Transient
    override val messageText: String = "今日商店"

    @Transient
    override val subscribeType: SubscribeType = SubscribeType.DAILY_STORE

    override suspend fun getUserSubscribeImage(userQQ: Long, userCache: UserCache) =
        userCache.getOrCacheImage(GenerateImageType.SKINS_PANEL_LAYOUT) {
            val storeFront = RiotApiHelper.queryStoreFrontApi(userCache)
            UserImageCacheCleanTask(
                storeFront.skinsPanelLayout.singleItemOffersRemainingDurationInSeconds.toDuration(DurationUnit.SECONDS),
                it,
                userQQ
            ).enable()
            ImageGenerator.storeImage(userCache, it, storeFront)
        }.getOrFail()


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