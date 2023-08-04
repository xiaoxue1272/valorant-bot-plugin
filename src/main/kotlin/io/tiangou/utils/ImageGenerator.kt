package io.tiangou.utils

import io.tiangou.DrawImageApiEnum
import io.tiangou.Global
import io.tiangou.logic.image.generator.AwtImageGenerator
import io.tiangou.logic.image.generator.SkikoImageGenerator
import io.tiangou.repository.UserCache
import java.util.concurrent.ConcurrentHashMap

internal interface ImageGenerator {

    suspend fun generateDailyStoreImage(userCache: UserCache): ByteArray

    suspend fun generateAccessoryStoreImage(userCache: UserCache): ByteArray

    companion object {

        internal val cacheSkinsPanelLayoutImages: ConcurrentHashMap<String, ByteArray> by lazy { ConcurrentHashMap() }

        internal val cacheAccessoryStoreImages: ConcurrentHashMap<String, ByteArray> by lazy { ConcurrentHashMap() }

        fun get(): ImageGenerator =
            when (Global.drawImageConfig.api) {
                DrawImageApiEnum.SKIKO -> SkikoImageGenerator
                DrawImageApiEnum.AWT -> AwtImageGenerator
            }

        suspend fun ImageGenerator.cache(key: String, cacheImages: MutableMap<String, ByteArray>, block: suspend ImageGenerator.() -> ByteArray) =
            cacheImages[key] ?: block().apply { cacheImages[key] = this }


        fun clean(userCache: UserCache) {
            userCache.riotClientData.puuid?.let {
                userCache.synchronized {
                    cacheSkinsPanelLayoutImages.remove(it)
                    cacheAccessoryStoreImages.remove(it)
                }
            }
        }

    }

}