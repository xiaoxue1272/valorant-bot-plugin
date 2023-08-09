package io.tiangou.other.image.skiko

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.other.http.client
import io.tiangou.other.image.GenerateStoreImageType
import io.tiangou.other.image.ImageGenerator
import io.tiangou.repository.UserCache
import io.tiangou.utils.*
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface

object SkikoImageGenerator : ImageGenerator {

    override suspend fun storeImage(userCache: UserCache, type: GenerateStoreImageType): ByteArray =
        Surface.makeByImageAndProportion(
            userCache.customBackgroundFile?.readBytes(),
            ImageGenerator.wp,
            ImageGenerator.hp
        ).afterClose { surface ->
            val storeFront = StoreApiHelper.queryStoreFront(userCache)
            val width = (surface.width - surface.width * 0.4f).toInt()
            val height = width / 2
            val imageList = when (type) {
                    GenerateStoreImageType.SKINS_PANEL_LAYOUT ->
                        SkinsPanelLayout.convert(storeFront).map { data ->
                            singleDailySkinImage(width, height, data)
                        }

                    GenerateStoreImageType.ACCESSORY_STORE ->
                        AccessoryStore.convert(storeFront).map { data ->
                            singleAccessoryItemImage(width, height, data)
                        }
                }
            imageList.forEachIndexed { index, it ->
                val lr = surface.width * 0.4f
                val imageWidth = surface.width - lr
                val imageHeight = it.height * (imageWidth / it.width)
                val top =
                    (surface.height - imageHeight * imageList.size - surface.height * 0.05f * (imageList.size - 1)) / 2
                writeImageRect(
                    it,
                    imageWidth,
                    imageHeight,
                    (lr / 2f),
                    (top + (imageHeight + surface.height * 0.05f) * index)
                )
            }
            surface.makeImageSnapshot().encodeToData()!!.bytes
        }

    private suspend fun singleDailySkinImage(width: Int, height: Int, data: SkinImageData): Image =
        Surface.makeRaster(ImageInfo.makeN32Premul(width, height)).afterClose {
            if (data.backgroundColor?.isNotEmpty() == true) {
                writeBackgroundColor(it, data.backgroundColor)
            }
            if (data.contentTiersUrl?.isNotEmpty() == true) {
                val contentTireSize = height / 6f
                writeImageRect(
                    client.get(data.contentTiersUrl).readBytes(),
                    contentTireSize,
                    contentTireSize,
                    width - contentTireSize
                )
            }
            if (data.themeUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(
                    client.get(data.themeUrl).readBytes(),
                    it,
                    alphaPercent = 0.5f
                )
            }
            if (data.skinUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(client.get(data.skinUrl).readBytes(), it, 0.3f)
            }
            if (data.skinName?.isNotEmpty() == true) {
                val fontSize = height / 12f
                writeHorizontallyAlignTextLine(data.skinName, width, fontSize, height.toFloat() / 2f + fontSize * 5)
            }
            it.flush()
            it.makeImageSnapshot()
        }

    private suspend fun singleAccessoryItemImage(width: Int, height: Int, data: AccessoryImageData): Image =
        Surface.makeRaster(ImageInfo.makeN32Premul(width, height)).afterClose {
            var heightY: Float = height / 2f
            val fontSize = height / 12f
            writeBackgroundColor(it, "#707070", 0.5f)
            if (data.itemUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(client.get(data.itemUrl).readBytes(), it)
                heightY = height - (fontSize + fontSize / 5)
            }
            if (data.itemName?.isNotEmpty() == true) {
                writeHorizontallyAlignTextLine(data.itemName, width, fontSize, heightY)
            }
            if (data.contractName?.isNotEmpty() == true) {
                writeHorizontallyAlignTextLine(data.contractName, width, fontSize, height.toFloat() - fontSize / 5)
            }
            it.flush()
            it.makeImageSnapshot()
        }

}