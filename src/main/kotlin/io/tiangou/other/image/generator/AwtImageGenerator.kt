package io.tiangou.other.image.generator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.other.http.client
import io.tiangou.other.image.awt.*
import io.tiangou.repository.UserCache
import io.tiangou.utils.*
import java.awt.RenderingHints
import java.awt.image.BufferedImage


object AwtImageGenerator : ImageGenerator {

    override suspend fun storeImage(userCache: UserCache, type: GenerateStoreImageType): ByteArray =
        readBackgroundImage(userCache.customBackgroundFile?.readBytes())
            .makeByImageAndProportion(ImageGenerator.wp, ImageGenerator.hp)
            .afterClose { image ->
                val storeFront = StoreApiHelper.queryStoreFront(userCache)
                val width = (image.width - image.width * 0.4f).toInt()
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
                    val lr = image.width * 0.4f
                    val imageWidth = image.width - lr
                    val imageHeight = it.height * (imageWidth / it.width)
                    val top =
                        (image.height - imageHeight * imageList.size - image.height * 0.05 * (imageList.size - 1)) / 2
                    writeImageRect(
                        it,
                        imageWidth.toInt(),
                        imageHeight.toInt(),
                        (lr / 2f).toInt(),
                        (top + (imageHeight + image.height * 0.05) * index).toInt()
                    )
                }
                image.writeImage()
            }


    private suspend fun singleDailySkinImage(width: Int, height: Int, data: SkinImageData): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .setBackgroundColor(data.backgroundColor)
        image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            if (data.contentTiersUrl?.isNotEmpty() == true) {
                val contentTireSize = height / 6
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
                    image,
                    alphaPercent = 0.5f
                )
            }
            if (data.skinUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(client.get(data.skinUrl).readBytes(), image, 0.3f)
            }
            if (data.skinName?.isNotEmpty() == true) {
                val fontSize = height / 12
                writeHorizontallyAlignTextLine(data.skinName, width, fontSize, height / 2 + fontSize * 5)
            }
        }.dispose()
        image.flush()
        return image
    }

    private suspend fun singleAccessoryItemImage(width: Int, height: Int, data: AccessoryImageData): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .setBackgroundColor("#70707080", 0.5f)
        image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            var heightY = height / 2
            val fontSize = height / 12
            if (data.itemUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(client.get(data.itemUrl).readBytes(), image)
                heightY = height - (fontSize + fontSize / 5)
            }
            if (data.itemName?.isNotEmpty() == true) {
                writeHorizontallyAlignTextLine(data.itemName, width, fontSize, heightY)
            }
            if (data.contractName?.isNotEmpty() == true) {
                writeHorizontallyAlignTextLine(data.contractName, width, fontSize, height - (fontSize / 5))
            }
        }.dispose()
        image.flush()
        return image
    }

}