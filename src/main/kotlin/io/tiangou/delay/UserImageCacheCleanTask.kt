package io.tiangou.delay

import io.tiangou.GenerateImageType
import io.tiangou.repository.UserCacheRepository
import kotlin.time.Duration

class UserImageCacheCleanTask(delay: Duration, private val type: GenerateImageType, private val userQQ: Long) :
    SingleDelayTask<UserImageCacheCleanTask.CleanImageCacheData>(delay) {

    override val description: String = "用户[${userQQ}]生成缓存图片[${type.value}]清理任务"

    class CleanImageCacheData(
        private val type: GenerateImageType,
        private val belongUser: Long,
    ) : Comparable<CleanImageCacheData> {
        override fun compareTo(other: CleanImageCacheData): Int {
            val typeCompareResult = type.compareTo(other.type)
            if (typeCompareResult != 0) return typeCompareResult
            val belongUserCompareResult = belongUser.compareTo(other.belongUser)
            if (belongUserCompareResult != 0) return belongUserCompareResult
            return 0
        }
    }

    override val condition: CleanImageCacheData = CleanImageCacheData(type, userQQ)

    override suspend fun execute() {
        UserCacheRepository[userQQ].synchronous { removeCacheImage(type) }
    }

}