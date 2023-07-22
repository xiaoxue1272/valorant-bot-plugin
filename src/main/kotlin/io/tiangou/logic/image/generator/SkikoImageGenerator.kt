package io.tiangou.logic.image.generator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.logic.image.utils.*
import io.tiangou.other.http.client
import io.tiangou.other.image.skiko.*
import io.tiangou.repository.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.skia.*

internal interface SkikoImageGenerator {

    suspend fun storeImage(background: Image, imageList: List<Image>): ByteArray {
        return runInterruptible(Dispatchers.IO) {
            Surface.makeByImageAndProportion(background, 9, 16).afterClose {
                canvas.apply {
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

    }

}

class SkikoSkinsPanelLayoutImageGenerator(private val userCache: UserCache) : SkikoImageGenerator, ImageHelper {

    override suspend fun generate(): ByteArray {
        val image = Image.makeFromEncoded(ImageHelper.getBackgroundFile(userCache))
        val width = (image.width - image.width * 0.4f).toInt()
        return storeImage(
            image,
            StoreApiHelper.querySkinsPanelLayout(userCache).map {
                singleSkinImage(width, width / 2, SkinsPanelLayout.getMediaData(it))
            }
        )
    }

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

class SkikoAccessoryImageGenerator(private val userCache: UserCache) : SkikoImageGenerator, ImageHelper {

    override suspend fun generate(): ByteArray {
        val image = Image.makeFromEncoded(ImageHelper.getBackgroundFile(userCache))
        val width = (image.width - image.width * 0.4f).toInt()
        return storeImage(
            image,
            StoreApiHelper.queryAccessoryStore(userCache).map {
                singleSkinImage(width, width / 2, Accessory.getMediaData(it))
            }
        )
    }

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