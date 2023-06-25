package io.tiangou.logic.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.ValorantBotPlugin
import io.tiangou.ValorantRuntimeException
import io.tiangou.api.ApiException
import io.tiangou.api.RiotApi
import io.tiangou.api.RiotClientData
import io.tiangou.api.data.StorefrontRequest
import io.tiangou.other.http.client
import io.tiangou.other.skiko.*
import io.tiangou.repository.ContentTier
import io.tiangou.repository.Theme
import io.tiangou.repository.WeaponSkin
import io.tiangou.repository.WeaponSkinLevel
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.*
import java.io.File

object DailyStoreImageGenerator {

    private val backgroundFile: File = ValorantBotPlugin.dataFolder.resolve("background.png").apply {
        if (!exists()) {
            writeBytes(runBlocking {
                client.get("https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg").readBytes()
            })
        }
    }

    suspend fun generate(client: RiotClientData): ByteArray {
        val skinImageList = client.queryDailyStoreSkinLevelUuidList().map {
            singleSkinImage(getSkinImageUrlData(it))
        }
        return storeImage(skinImageList)
    }

    private suspend fun RiotClientData.queryDailyStoreSkinLevelUuidList(): List<String> {
        try {
            flushAccessToken(RiotApi.CookieReAuth.execute())
            flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            val puuid = puuid.get() ?: RiotApi.PlayerInfo.execute().sub.also { puuid.set(it) }
            return RiotApi.Storefront.execute(StorefrontRequest(shard!!, puuid)).skinsPanelLayout.singleItemOffers
        } catch (e: ApiException) {
            throw ValorantRuntimeException("错误信息:${e.message},可能是账号登录过期,请重新登录账号后重试")
        }
    }


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

    private fun storeImage(skinImageList: List<Image>): ByteArray {
        val image = Image.makeFromEncoded(backgroundFile.readBytes())
        return Surface.makeByImageAndProportion(image, 9, 16).afterClose {
            canvas.writeImageRect(image, (width - image.width) / 2f, (height - image.height) / 2f).apply {
                skinImageList.forEachIndexed { index, it ->
                    val lr = width * 0.4f
                    val imageWidth = width - lr
                    val imageHeight = it.height * (imageWidth / it.width)
                    val top =
                        (height - (imageHeight * skinImageList.size + height * 0.05f * (skinImageList.size - 1))) / 2
                    writeImageRect(it, imageWidth, imageHeight, lr / 2f, top + (imageHeight + height * 0.05f) * index)
                }
            }
            makeImageSnapshot().encodeToData()!!.bytes
        }
    }

}