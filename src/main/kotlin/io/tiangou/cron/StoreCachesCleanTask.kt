package io.tiangou.cron

import cn.hutool.cron.pattern.CronPattern
import cn.hutool.cron.pattern.CronPatternUtil
import io.ktor.util.date.*
import io.tiangou.logic.utils.StoreApiHelper
import io.tiangou.logic.utils.StoreImageHelper
import io.tiangou.repository.UserCacheRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

object StoreCachesCleanTask : Task() {

    override val description: String = "生成缓存图片清理"

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
        cronPattern = CronPattern.of(cron)
        job = launch {
            while (true) {
                val nowTimeMillis = getTimeMillis()
                val waitOnExecuteTimeMillis =
                    CronPatternUtil.nextDateAfter(cronPattern, Date(), true).time - nowTimeMillis
                delay(waitOnExecuteTimeMillis)
                val startTimeMillis = getTimeMillis()
                executeRunCaching()
                val remainingTime = getTimeMillis() - startTimeMillis
                if (remainingTime < 1000) {
                    delay(1000 - remainingTime)
                }
            }
        }
        log.info("已启用任务 [${this::class.simpleName}]")
        log.warning("请注意,该任务为后台任务,无法停用,启用,手动触发和修改")
        job?.invokeOnCompletion { if (it != null) log.info("已取消任务 [StoreCachesCleanTask]") }
    }
}