package io.tiangou.cron

import io.tiangou.GenerateImageType
import io.tiangou.api.RiotApiHelper
import io.tiangou.delay.UserImageCacheCleanTask
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class SubscribeWeeklyAccessoryStoreCronTask(
    override var cron: String,
    override var isEnable: Boolean
): AbstractSubscribeCronTask() {


    override val description: String = "每周配件商店推送"

    override val messageText: String = "本周配件商店"

    override suspend fun getUserSubscribeImage(userQQ: Long, userCache: UserCache): ByteArray =
        userCache.getOrCacheImage(GenerateImageType.ACCESSORY_STORE) {
            val storeFront = RiotApiHelper.queryStoreFrontApi(userCache)
            UserImageCacheCleanTask(
                storeFront.accessoryStore.accessoryStoreRemainingDurationInSeconds.toDuration(DurationUnit.SECONDS),
                it,
                userQQ
            ).enable()
            ImageGenerator.storeImage(userCache, it, storeFront)
        }.getOrFail()

}