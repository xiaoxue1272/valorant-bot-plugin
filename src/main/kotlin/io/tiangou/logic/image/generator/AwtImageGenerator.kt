package io.tiangou.logic.image.generator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.other.http.client
import io.tiangou.other.image.awt.*
import io.tiangou.repository.UserCache
import io.tiangou.utils.*
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File


object AwtImageGenerator: ImageGenerator {

    override suspend fun generateDailyStoreImage(userCache: UserCache): ByteArray =
        storeImage(userCache.customBackgroundFile) {
                val width = (it.width - it.width * 0.4f).toInt()
                val height = width / 2
                SkinsPanelLayout.convert(StoreApiHelper.queryStoreFront(userCache)).map { data ->
                    singleDailySkinImage(width, height, data)
                }
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
                setFont(fontSize)
                drawString(
                    data.skinName,
                    width / 2 - fontMetrics.stringWidth(data.skinName) / 2,
                    height / 2 + fontSize * 5
                )
            }
        }.dispose()
        image.flush()
        return image
    }

    override suspend fun generateAccessoryStoreImage(userCache: UserCache): ByteArray =
        storeImage(userCache.customBackgroundFile) { image ->
                val width = (image.width - image.width * 0.4f).toInt()
                val height = width / 2
                val storeFront = StoreApiHelper.queryStoreFront(userCache)
                AccessoryStore.convert(storeFront).map { data ->
                    singleAccessoryItemImage(width, height, data)
                }
            }

    private suspend fun singleAccessoryItemImage(width: Int, height: Int, data: AccessoryImageData): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .setBackgroundColor("#70707080", 0.5f)
        image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val textX = width / 2
            var heightY = height / 2
            val fontSize = height / 12
            setFont(height / 12)
            if (data.itemUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(client.get(data.itemUrl).readBytes(), image)
                heightY = height - (fontSize + fontSize / 5)
            }
            if (data.itemName?.isNotEmpty() == true) {
                drawString(
                    data.itemName,
                    textX - fontMetrics.stringWidth(data.itemName) / 2,
                    heightY
                )
            }
            if (data.contractName?.isNotEmpty() == true) {
                drawString(
                    data.contractName,
                    textX - fontMetrics.stringWidth(data.contractName) / 2,
                    height - (fontSize / 5)
                )
            }
        }.dispose()
        image.flush()
        return image
    }

    private suspend fun storeImage(backgroundFile: File?, block: suspend Graphics2D.(BufferedImage) -> List<BufferedImage>): ByteArray =
        DrawImageApiAdpater.drawAwtOnBackground(backgroundFile) { image ->
            val imageList = block(this, image)
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

}