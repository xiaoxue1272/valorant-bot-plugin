package io.tiangou.cron

import io.tiangou.logic.utils.GenerateImageType
import io.tiangou.logic.utils.StoreImageHelper
import io.tiangou.repository.UserCacheRepository
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.Image.Key.isUploaded
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
class DailyStorePushTask(
    override var cron: String,
    override var isEnable: Boolean
) : Task() {


    override val description: String = "每日商店推送"

    override suspend fun execute() {
        log.info("每日商店定时推送任务,开始")
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
        Bot.instances.forEach { bot ->
            UserCacheRepository.getAllUserCache().forEach { entry ->
                if (entry.value.subscribeDailyStore) {
                    val user = bot.getFriend(entry.key) ?: bot.getStranger(entry.key)
                    if (user == null) {
                        log.info("QQ:[${entry.key}],未在机器人联系列表中找到用户,请添加机器人为好友")
                    } else {
                        user.runCatching {
                            val skinsPanelLayoutImage =
                                StoreImageHelper.get(entry.value, GenerateImageType.SKINS_PANEL_LAYOUT)
                            var uploadImage = uploadImage(skinsPanelLayoutImage.toExternalResource().toAutoCloseable())
                            repeat(2) {
                                if (!uploadImage.isUploaded(bot)) {
                                    uploadImage =
                                        uploadImage(skinsPanelLayoutImage.toExternalResource().toAutoCloseable())
                                }
                            }
                            if (uploadImage.isUploaded(bot)) {
                                val message = MessageChainBuilder()
                                    .append("$date 今日商店为:\n")
                                    .append(uploadImage)
                                    .build()
                                sendMessage(message)
                            }
                        }.onFailure {
                            log.warning("QQ:[${entry.key}],推送每日商店内容异常,异常信息:", it)
                        }
                    }
                }
            }
        }
        log.info("每日商店定时推送任务,结束")
    }

}