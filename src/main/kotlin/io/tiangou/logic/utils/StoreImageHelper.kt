package io.tiangou.logic.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.ValorantBotPlugin
import io.tiangou.api.RiotApi
import io.tiangou.api.data.StoreFrontResponse
import io.tiangou.api.data.StorefrontRequest
import io.tiangou.logic.utils.StoreImageHelper.Companion.getBackgroundFile
import io.tiangou.logic.utils.StoreImageHelper.Companion.storeImage
import io.tiangou.other.http.actions
import io.tiangou.other.http.client
import io.tiangou.other.skiko.*
import io.tiangou.repository.UserCache
import io.tiangou.repository.persistnce.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal sealed interface StoreImageHelper {

    suspend fun generate(): ByteArray

    fun synchronousGenerate() = runBlocking { generate() }

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
            val key = userCache.riotClientData.puuid!!
            return synchronized(userCache) {
                when (type) {
                    GenerateImageType.SKINS_PANEL_LAYOUT -> cacheSkinsPanelLayoutImages[key]
                        ?: SkinsPanelLayout(userCache).synchronousGenerate()
                            .apply { cacheSkinsPanelLayoutImages[key] = this }

                    GenerateImageType.ACCESSORY_STORE -> cacheAccessoryStoreImages[key]
                        ?: AccessoryStore(userCache).synchronousGenerate()
                            .apply { cacheAccessoryStoreImages[key] = this }
                }
            }
        }

        fun clean(userCache: UserCache) {
            userCache.riotClientData.puuid?.let {
                synchronized(userCache) {
                    cacheSkinsPanelLayoutImages.remove(it)
                    cacheAccessoryStoreImages.remove(it)
                }
            }
        }

        internal fun storeImage(background: Image, imageList: List<Image>): ByteArray {
            return Surface.makeByImageAndProportion(background, 9, 16).afterClose {
                canvas.writeImageRect(background, (width - background.width) / 2f, (height - background.height) / 2f)
                    .apply {
                        imageList.forEachIndexed { index, it ->
                            val lr = width * 0.4f
                            val imageWidth = width - lr
                            val imageHeight = it.height * (imageWidth / it.width)
                            val top =
                                (height - imageHeight * imageList.size - height * 0.05f * (imageList.size - 1)) / 2
                            writeImageRect(
                                it,
                                imageWidth,
                                imageHeight,
                                lr / 2f,
                                top + (imageHeight + height * 0.05f) * index
                            ).save()
                        }
                    }
                makeImageSnapshot().encodeToData()!!.bytes
            }
        }

        fun getBackgroundFile(userCache: UserCache) =
            userCache.customBackgroundFile?.let { Image.makeFromEncoded(it.readBytes()) }
                ?: Image.makeFromEncoded(globalBackground.readBytes())

    }

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

class SkinsPanelLayout(private val userCache: UserCache) : StoreImageHelper {

    override suspend fun generate(): ByteArray {
        val image = getBackgroundFile(userCache)
        val width = (image.width - image.width * 0.4f).toInt()
        return storeImage(
            image,
            StoreApiHelper.querySkinsPanelLayout(userCache).map {
                singleSkinImage(width, width / 2, getSkinImageUrlData(it))
            }
        )
    }

    private fun getSkinImageUrlData(skinLevelUuid: String): SkinImageData {
        val skinLevel = WeaponSkinLevel(skinLevelUuid).queryOne()
        val skin = WeaponSkin(skinLevel?.weaponSkinUuid).queryOne()
        val theme = skin?.themeUuid?.let { Theme(it).queryOne() }
        val contentTier = skin?.contentTiersUuid?.let { ContentTier(it).queryOne() }
        return SkinImageData(
            skinLevel?.displayIcon,
            contentTier?.displayIcon,
            theme?.displayIcon,
            contentTier?.highlightColor,
            skinLevel?.displayName
        )
    }

    private data class SkinImageData(
        val skinUrl: String?,
        val contentTiersUrl: String?,
        val themeUrl: String?,
        val backgroundColor: String?,
        val skinName: String?
    )

    private suspend fun singleSkinImage(width: Int, height: Int, data: SkinImageData): Image =
        Surface.makeRaster(ImageInfo.makeN32Premul(width, height)).afterClose {
            if (data.backgroundColor?.isNotEmpty() == true) {
                canvas.writeBackgroundColor(this@afterClose, data.backgroundColor, 0.5f)
            }
            if (data.contentTiersUrl?.isNotEmpty() == true) {
                val contentTireSize = height / 6f
                canvas.writeImageRect(
                    client.get(data.contentTiersUrl).readBytes(),
                    contentTireSize,
                    contentTireSize,
                    width - contentTireSize
                )
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
                val fontSize = height / 12f
                val textLine = TextLine.make(data.skinName, Font(Typeface.makeDefault(), fontSize))
                canvas.drawTextLine(
                    textLine,
                    width.toFloat() / 2f - textLine.width / 2f,
                    height.toFloat() / 2f + fontSize * 5,
                    Paint().apply { color = Color.WHITE })
            }
            flush()
            makeImageSnapshot()
        }

}

class AccessoryStore(private val userCache: UserCache) : StoreImageHelper {

    override suspend fun generate(): ByteArray {
        val image = getBackgroundFile(userCache)
        val width = (image.width - image.width * 0.4f).toInt()
        return storeImage(
            image,
            StoreApiHelper.queryAccessoryStore(userCache).map {
                singleSkinImage(width, width / 2, getAccessoryImageUrlData(it))
            }
        )
    }

    private fun getAccessoryImageUrlData(accessory: StoreFrontResponse.AccessoryStore.AccessoryStoreOffer): AccessoryImageData {
        val contract = Contract(accessory.contractID).queryOne()
        val itemID = accessory.offer.rewards.first().itemID
        val itemType = ContractLevel(itemID, accessory.contractID).queryOne()?.type?.let { AccessoryItemType.match(it) }
        var itemUrl: String? = null
        var itemName: String? = null
        when (itemType) {
            AccessoryItemType.CURRENCY -> Currency(itemID).queryOne()
                ?.apply { itemUrl = largeIcon; itemName = displayName }

            AccessoryItemType.PENDANT -> {
                val buddieLevel = BuddieLevel(itemID).queryOne()
                buddieLevel?.buddieUuid.let { Buddie(it) }.queryOne()
                    ?.apply { itemUrl = displayIcon; itemName = displayName }
            }

            AccessoryItemType.SKIN -> WeaponSkinLevel(itemID).queryOne()
                ?.apply { itemUrl = displayIcon; itemName = displayName }

            AccessoryItemType.PLAYER_CARD -> PlayCard(itemID).queryOne()
                ?.apply { itemUrl = displayIcon; itemName = displayName }

            AccessoryItemType.SPRAY -> Spray(itemID).queryOne()?.apply { itemUrl = displayIcon; itemName = displayName }
            AccessoryItemType.TITLE -> PlayTitle(itemID).queryOne()?.apply { itemName = displayName }
            null -> {}
        }
        return AccessoryImageData(itemUrl, itemName, contract?.displayName)
    }

    private data class AccessoryImageData(
        val itemUrl: String?,
        val itemName: String?,
        val contractName: String?,
    )

    private suspend fun singleSkinImage(width: Int, height: Int, data: AccessoryImageData): Image =
        Surface.makeRaster(ImageInfo.makeN32Premul(width, height)).afterClose {
            val textX: Float = width / 2f
            var heightY: Float = height / 2f
            val fontSize = height / 12f
            canvas.writeBackgroundColor(this@afterClose, "#707070", 0.5f)
            if (data.itemUrl?.isNotEmpty() == true) {
                canvas.writeAutoSizeImage(client.get(data.itemUrl).readBytes(), this@afterClose)
                heightY = height - (fontSize + fontSize / 5)
            }
            if (data.itemName?.isNotEmpty() == true) {
                val textLine = TextLine.make(data.itemName, Font(Typeface.makeDefault(), fontSize))
                canvas.drawTextLine(
                    textLine,
                    textX - textLine.width / 2f,
                    heightY,
                    Paint().apply { color = Color.WHITE }
                )
            }
            if (data.contractName?.isNotEmpty() == true) {
                val textLine =
                    TextLine.make(data.contractName, Font(Typeface.makeDefault(), fontSize))
                canvas.drawTextLine(
                    textLine,
                    textX - textLine.width / 2f,
                    height.toFloat() - fontSize / 5,
                    Paint().apply { color = Color.WHITE }
                )
            }
            flush()
            makeImageSnapshot()
        }

}

enum class AccessoryItemType(val value: String) {
    CURRENCY("Currency"),
    PENDANT("EquippableCharmLevel"),
    SKIN("EquippableSkinLevel"),
    PLAYER_CARD("PlayerCard"),
    SPRAY("Spray"),
    TITLE("Title"),
    ;

    companion object {
        fun match(value: String): AccessoryItemType {
            values().forEach {
                if (it.value == value) return it
            }
            throw IllegalArgumentException("value is not in AccessoryItemType")
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