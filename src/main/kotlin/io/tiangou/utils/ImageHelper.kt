package io.tiangou.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.DrawImageApiEnum
import io.tiangou.Global
import io.tiangou.ValorantBotPlugin
import io.tiangou.logic.image.generator.AwtAccessoryImageGenerator
import io.tiangou.logic.image.generator.AwtSkinsPanelLayoutImageGenerator
import io.tiangou.logic.image.generator.SkikoAccessoryImageGenerator
import io.tiangou.logic.image.generator.SkikoSkinsPanelLayoutImageGenerator
import io.tiangou.other.http.client
import io.tiangou.repository.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal interface ImageHelper {

    suspend fun generate(): ByteArray

    fun synchronousGenerate() = runBlocking(Dispatchers.IO) { generate() }

    companion object {

        private val globalBackground: File = ValorantBotPlugin.dataFolder.resolve("background.png").apply {
            if (!exists()) {
                writeBytes(runBlocking {
                    client.get("https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg")
                        .readBytes()
                })
            }
        }

        internal val cacheSkinsPanelLayoutImages: ConcurrentHashMap<String, ByteArray> by lazy { ConcurrentHashMap() }

        internal val cacheAccessoryStoreImages: ConcurrentHashMap<String, ByteArray> by lazy { ConcurrentHashMap() }

        fun get(userCache: UserCache, type: GenerateImageType): ByteArray {
            return userCache.synchronized {
                val key = userCache.riotClientData.puuid!!
                when (Global.drawImageConfig.api) {
                    DrawImageApiEnum.SKIKO -> skikoGenerate(userCache, key, type)
                    DrawImageApiEnum.AWT -> awtGenerate(userCache, key, type)
                }
            }
        }

        private fun skikoGenerate(userCache: UserCache, key: String, type: GenerateImageType): ByteArray {
            return when (type) {
                GenerateImageType.SKINS_PANEL_LAYOUT -> cacheSkinsPanelLayoutImages[key]
                    ?: SkikoSkinsPanelLayoutImageGenerator(userCache).synchronousGenerate()
                        .apply { cacheSkinsPanelLayoutImages[key] = this }

                GenerateImageType.ACCESSORY_STORE -> cacheAccessoryStoreImages[key]
                    ?: SkikoAccessoryImageGenerator(userCache).synchronousGenerate()
                        .apply { cacheAccessoryStoreImages[key] = this }
            }
        }

        private fun awtGenerate(userCache: UserCache, key: String, type: GenerateImageType): ByteArray {
            return when (type) {
                GenerateImageType.SKINS_PANEL_LAYOUT -> cacheSkinsPanelLayoutImages[key]
                    ?: AwtSkinsPanelLayoutImageGenerator(userCache).synchronousGenerate()
                        .apply { cacheSkinsPanelLayoutImages[key] = this }

                GenerateImageType.ACCESSORY_STORE -> cacheAccessoryStoreImages[key]
                    ?: AwtAccessoryImageGenerator(userCache).synchronousGenerate()
                        .apply { cacheAccessoryStoreImages[key] = this }
            }
        }

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


/**
 * 生成图片类型
 */
enum class GenerateImageType {

    SKINS_PANEL_LAYOUT,
    ACCESSORY_STORE,

}