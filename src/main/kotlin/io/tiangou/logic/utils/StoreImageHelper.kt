package io.tiangou.logic.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.ValorantBotPlugin
import io.tiangou.api.RiotApi
import io.tiangou.api.data.StoreFrontResponse
import io.tiangou.api.data.StorefrontRequest
import io.tiangou.other.http.actions
import io.tiangou.other.http.client
import io.tiangou.other.skiko.*
import io.tiangou.repository.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

sealed class StoreImageHelper {

    abstract suspend fun generate(): ByteArray

    protected fun storeImage(imageList: List<Image>): ByteArray {
        val image = Image.makeFromEncoded(globalBackground.readBytes())
        return Surface.makeByImageAndProportion(image, 9, 16).afterClose {
            canvas.writeImageRect(image, (width - image.width) / 2f, (height - image.height) / 2f).apply {
                imageList.forEachIndexed { index, it ->
                    val lr = width * 0.4f
                    val imageWidth = width - lr
                    val imageHeight = it.height * (imageWidth / it.width)
                    val top =
                        (height - (imageHeight * imageList.size + height * 0.05f * (imageList.size - 1))) / 2
                    writeImageRect(it, imageWidth, imageHeight, lr / 2f, top + (imageHeight + height * 0.05f) * index)
                }
            }
            makeImageSnapshot().encodeToData()!!.bytes
        }
    }

    data class CacheImages(
        var skinsPanelLayout: ByteArray? = null,
        var accessoryStore: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CacheImages

            if (skinsPanelLayout != null) {
                if (other.skinsPanelLayout == null) return false
                if (!skinsPanelLayout.contentEquals(other.skinsPanelLayout)) return false
            } else if (other.skinsPanelLayout != null) return false
            if (accessoryStore != null) {
                if (other.accessoryStore == null) return false
                if (!accessoryStore.contentEquals(other.accessoryStore)) return false
            } else if (other.accessoryStore != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = skinsPanelLayout?.contentHashCode() ?: 0
            result = 31 * result + (accessoryStore?.contentHashCode() ?: 0)
            return result
        }
    }

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

        suspend fun get(userCache: UserCache, type: GenerateImageType): ByteArray {
            return when (type) {
                GenerateImageType.SKINS_PANEL_LAYOUT -> cacheSkinsPanelLayoutImages[userCache.riotClientData.puuid!!]
                    ?: SkinsPanelLayout(userCache).generate()
                        .apply { cacheSkinsPanelLayoutImages[userCache.riotClientData.puuid!!] = this }

                GenerateImageType.ACCESSORY_STORE -> cacheAccessoryStoreImages[userCache.riotClientData.puuid!!]
                    ?: SkinsPanelLayout(userCache).generate()
                        .apply { cacheAccessoryStoreImages[userCache.riotClientData.puuid!!] = this }
            }
        }


    }

    // todo 配件查询功能
//    @JvmInline
//    value class AccessoryStore(private val userCache: UserCache): StoreImageHelper {
//
//        override suspend fun generate(): ByteArray = storeImage(
//            StoreApiHelper.queryAccessoryStore(userCache).map {
//                singleSkinImage(getAccessoryImageUrlData(it))
//            }
//        )
//
//        private fun getAccessoryImageUrlData(accessory: StoreFrontResponse.AccessoryStore.AccessoryStoreOffer): AccessoryImageData {
//            accessory.contractID
//            return AccessoryImageData(
//                skinLevel.displayIcon,
//                contentTier?.displayIcon,
//                theme?.displayIcon,
//                skinLevel.displayName
//            )
//        }
//
//        private data class AccessoryImageData(
//            val skinUrl: String?,
//            val contentTiersUrl: String?,
//            val themeUrl: String?,
//            val skinName: String?
//        )
//
//        private fun singleSkinImage(data: AccessoryImageData): Image =
//            Surface.makeRaster(ImageInfo.makeN32Premul(1200, 600)).afterClose {
//                runBlocking {
//                    canvas.writeBackgroundColor(this@afterClose, "#707070", 0.5f)
//                    if (data.contentTiersUrl?.isNotEmpty() == true) {
//                        canvas.writeImageRect(client.get(data.contentTiersUrl).readBytes(), 100f, 100f, 1100f)
//                    }
//                    if (data.themeUrl?.isNotEmpty() == true) {
//                        canvas.writeAutoSizeImage(
//                            client.get(data.themeUrl).readBytes(),
//                            this@afterClose,
//                            alphaPercent = 0.5f
//                        )
//                    }
//                    if (data.skinUrl?.isNotEmpty() == true) {
//                        canvas.writeAutoSizeImage(client.get(data.skinUrl).readBytes(), this@afterClose, 0.3f)
//                    }
//                    if (data.skinName?.isNotEmpty() == true) {
//                        val textLine = TextLine.make(data.skinName, Font(Typeface.makeDefault(), 60f))
//                        canvas.drawTextLine(
//                            textLine,
//                            width.toFloat() / 2f - textLine.width / 2f,
//                            height.toFloat() / 2f + 250f,
//                            Paint().apply { color = Color.WHITE })
//                    }
//                    flush()
//                }
//                makeImageSnapshot()
//            }
//
//    }

}


object StoreApiHelper {

    private suspend fun invokeRiotApi(userCache: UserCache) =
        userCache.riotClientData.actions {
            flushAccessToken(RiotApi.CookieReAuth.execute())
            flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            RiotApi.Storefront.execute(StorefrontRequest(shard!!, puuid!!))
        }.apply { storeFronts[userCache.riotClientData.puuid!!] = this }

    suspend fun querySkinsPanelLayout(userCache: UserCache): List<String> {
        return storeFronts[userCache.riotClientData.puuid!!]?.skinsPanelLayout?.singleItemOffers ?: invokeRiotApi(
            userCache
        ).skinsPanelLayout.singleItemOffers
    }

    suspend fun queryAccessoryStore(userCache: UserCache): List<StoreFrontResponse.AccessoryStore.AccessoryStoreOffer> {
        return storeFronts[userCache.riotClientData.puuid!!]?.accessoryStore?.accessoryStoreOffers ?: invokeRiotApi(
            userCache
        ).accessoryStore.accessoryStoreOffers
    }

    internal val storeFronts: ConcurrentHashMap<String, StoreFrontResponse> by lazy { ConcurrentHashMap() }


}

class SkinsPanelLayout(private val userCache: UserCache) : StoreImageHelper() {

    override suspend fun generate(): ByteArray = storeImage(
        StoreApiHelper.querySkinsPanelLayout(userCache).map { singleSkinImage(getSkinImageUrlData(it)) }
    )

    private fun getSkinImageUrlData(skinLevelUuid: String): SkinImageData {
        val skinLevel = WeaponSkinLevel(skinLevelUuid).queryOne()
        val skin = WeaponSkin(skinLevel.weaponSkinUuid).queryOne()
        val theme = skin.themeUuid?.let { Theme(it).queryOne() }
        val contentTier = skin.contentTiersUuid?.let { ContentTier(it).queryOne() }
        return SkinImageData(
            skinLevel.displayIcon,
            contentTier?.displayIcon,
            theme?.displayIcon,
            contentTier?.highlightColor,
            skinLevel.displayName
        )
    }

    private data class SkinImageData(
        val skinUrl: String?,
        val contentTiersUrl: String?,
        val themeUrl: String?,
        val backgroundColor: String?,
        val skinName: String?
    )

    private fun singleSkinImage(data: SkinImageData): Image =
        Surface.makeRaster(ImageInfo.makeN32Premul(1200, 600)).afterClose {
            runBlocking {
                if (data.backgroundColor?.isNotEmpty() == true) {
                    canvas.writeBackgroundColor(this@afterClose, data.backgroundColor, 0.5f)
                }
                if (data.contentTiersUrl?.isNotEmpty() == true) {
                    canvas.writeImageRect(client.get(data.contentTiersUrl).readBytes(), 100f, 100f, 1100f)
                }
                if (data.themeUrl?.isNotEmpty() == true) {
                    canvas.writeAutoSizeImage(
                        client.get(data.themeUrl).readBytes(),
                        this@afterClose,
                        alphaPercent = 0.5f
                    )
                }
                if (data.skinUrl?.isNotEmpty() == true) {
                    canvas.writeAutoSizeImage(client.get(data.skinUrl).readBytes(), this@afterClose, 0.3f)
                }
                if (data.skinName?.isNotEmpty() == true) {
                    val textLine = TextLine.make(data.skinName, Font(Typeface.makeDefault(), 60f))
                    canvas.drawTextLine(
                        textLine,
                        width.toFloat() / 2f - textLine.width / 2f,
                        height.toFloat() / 2f + 250f,
                        Paint().apply { color = Color.WHITE })
                }
                flush()
            }
            makeImageSnapshot()
        }

}

/**
 * 生成图片类型
 */
enum class GenerateImageType {

    SKINS_PANEL_LAYOUT,
    ACCESSORY_STORE,

}