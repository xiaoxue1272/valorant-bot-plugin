package io.tiangou.cron

import io.ktor.util.date.*
import io.tiangou.GenerateImageType
import io.tiangou.api.StoreApiHelper
import io.tiangou.repository.UserCacheRepository
import java.time.ZoneId
import java.util.*

object CachesCleanCronTask : CronTask() {

    override val description: String = "生成缓存清理"

    override val timeZone: TimeZone = TimeZone.getTimeZone(ZoneId.of("GMT+8"))

    override var cron: String = "0 0 08 * * ? *"
        set(value) {
            throw IllegalArgumentException("cannot set a new value: $value to this field")
        }

    override var isEnable: Boolean = true
        set(value) {
            throw IllegalArgumentException("cannot set a new value: $value to this field")
        }

    override suspend fun execute() {
        UserCacheRepository.getAllUserCache().forEach {
            it.value.synchronized {
                StoreApiHelper.clean(this)
//                ImageGenerator.clean(this, GenerateImageType.SKINS_PANEL_LAYOUT)
                generateImages.remove(GenerateImageType.SKINS_PANEL_LAYOUT)
                if (GMTDate().dayOfWeek == WeekDay.WEDNESDAY) {
//                    ImageGenerator.clean(this, GenerateImageType.ACCESSORY_STORE)
                    generateImages.remove(GenerateImageType.ACCESSORY_STORE)
                }
            }
        }
    }

    override fun enable() {
        super.enable()
        log.info("已启用任务 [${this::class.simpleName}]")
        log.warning("请注意,该任务为后台任务,无法停用,启用,手动触发和修改")
        job?.invokeOnCompletion { if (it != null) log.info("已取消任务 [CachesCleanCronTask]") }
    }
}