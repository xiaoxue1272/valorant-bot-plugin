package io.tiangou.cron

import io.ktor.util.date.*
import io.tiangou.logic.utils.StoreApiHelper
import io.tiangou.logic.utils.StoreImageHelper
import io.tiangou.repository.UserCacheRepository

object StoreCachesCleanTask : Task() {

    override var cron: String = "0 0 08 * * ? *"
        set(value) {
            throw IllegalArgumentException("cannot set a new value: $value to this field")
        }

    override var isEnable: Boolean = true
        set(value) {
            throw IllegalArgumentException("cannot set a new value: $value to this field")
        }

    override suspend fun execute() {
        log.info("清理每日商店生成图片及信息")
        UserCacheRepository.getAllUserCache().forEach {
            synchronized(it) {
                StoreApiHelper.storeFronts.clear()
                StoreImageHelper.cacheSkinsPanelLayoutImages.clear()
                if (GMTDate().dayOfWeek == WeekDay.WEDNESDAY) {
                    StoreImageHelper.cacheAccessoryStoreImages.clear()
                }
            }
        }
        log.info("清理完成")
    }

    override fun enable() {
        super.enable()
        log.warning("请注意,该任务为后台任务,无法停用,启用,手动触发和修改")
        job?.invokeOnCompletion { if (it != null) log.info("已取消任务 [StoreCachesCleanTask]") }
    }
}