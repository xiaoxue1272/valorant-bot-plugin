package io.tiangou.other.image

import io.tiangou.DrawImageApiEnum
import io.tiangou.Global
import io.tiangou.other.image.awt.AwtImageGenerator
import io.tiangou.other.image.skiko.SkikoImageGenerator
import io.tiangou.repository.UserCache
import io.tiangou.utils.ByteArrayCache
import io.tiangou.utils.CacheFactory
import java.util.concurrent.ConcurrentHashMap

interface ImageGenerator {

    suspend fun storeImage(userCache: UserCache, type: GenerateStoreImageType): ByteArray

    companion object INSTANCE: ImageGenerator by when (Global.drawImageConfig.api) {
        DrawImageApiEnum.SKIKO -> SkikoImageGenerator
        DrawImageApiEnum.AWT -> AwtImageGenerator
    } {

        internal val wp: Int = 9

        internal val hp: Int = 16

        internal val caches: ConcurrentHashMap<String, MutableMap<GenerateStoreImageType, ByteArrayCache>> by lazy { ConcurrentHashMap() }

        suspend fun getCacheOrGenerate(
            userCache: UserCache,
            type: GenerateStoreImageType,
            block: suspend ImageGenerator.(GenerateStoreImageType) -> ByteArray
        ): ByteArray = userCache.synchronous {
            val key = riotClientData.puuid!!
            val cacheTypeElements = caches[key]
                ?: mutableMapOf<GenerateStoreImageType, ByteArrayCache>().apply { caches[key] = this }
            val cache = cacheTypeElements[type] ?: CacheFactory.create().apply {
                cacheTypeElements[type] = this
            }
            cache.get()?.takeIf { it.isNotEmpty() } ?: block(this@INSTANCE, type).apply { cache.put(this) }
        }

        fun clean(userCache: UserCache) {
            userCache.riotClientData.puuid?.let { caches[it] }?.forEach { it.value.clean() }
        }

        fun clean(userCache: UserCache, type: GenerateStoreImageType) {
            userCache.riotClientData.puuid?.let { caches[it] }?.get(type)?.clean()
        }

    }

}

/**
 * 生成图片类型
 */
enum class GenerateStoreImageType {

    SKINS_PANEL_LAYOUT,
    ACCESSORY_STORE,

}