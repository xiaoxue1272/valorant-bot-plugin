package io.tiangou.cron

import io.ktor.util.date.*
import io.tiangou.repository.UserCacheRepository
import io.tiangou.utils.ImageHelper
import io.tiangou.utils.StoreApiHelper
import java.time.ZoneId
import java.util.*

object StoreCachesCleanTask : Task() {

    override val description: String = "生成缓存图片清理"

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
                StoreApiHelper.storeFronts.clear()
                ImageHelper.cacheSkinsPanelLayoutImages.clear()
                if (GMTDate().dayOfWeek == WeekDay.WEDNESDAY) {
                    ImageHelper.cacheAccessoryStoreImages.clear()
                }
            }
        }
    }

    override fun enable() {
        super.enable()
        log.info("已启用任务 [${this::class.simpleName}]")
        log.warning("请注意,该任务为后台任务,无法停用,启用,手动触发和修改")
        job?.invokeOnCompletion { if (it != null) log.info("已取消任务 [StoreCachesCleanTask]") }
    }
}