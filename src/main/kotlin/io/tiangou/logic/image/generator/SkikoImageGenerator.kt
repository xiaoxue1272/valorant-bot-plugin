package io.tiangou.logic.image.generator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.other.http.client
import io.tiangou.other.image.skiko.*
import io.tiangou.repository.UserCache
import io.tiangou.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface

abstract class SkikoImageGenerator {


    suspend fun storeImage(background: Image, imageList: List<Image>): ByteArray {
        return runInterruptible(Dispatchers.IO) {
            Surface.makeByImageAndProportion(background, 9, 16).afterClose { surface ->
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
                        lr / 2f,
                        top + (imageHeight + surface.height * 0.05f) * index
                    ).save()
                }
                surface.makeImageSnapshot().encodeToData()!!.bytes
            }
        }

    }

}

class SkikoSkinsPanelLayoutImageGenerator(private val userCache: UserCache) : SkikoImageGenerator(), ImageHelper {

    override suspend fun generate(): ByteArray {
        val image = DrawImageApiAdpater.getSkikoBackground(userCache.customBackgroundFile).makeImageSnapshot()
        val width = (image.width - image.width * 0.4f).toInt()
        return storeImage(
            image,
            SkinsPanelLayout.convert(StoreApiHelper.queryStoreFront(userCache)).map {
                singleSkinImage(width, width / 2, it)
            }
        )
    }

    private suspend fun singleSkinImage(width: Int, height: Int, data: SkinImageData): Image =
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
                val textLine = makeTextLine(data.skinName, fontSize)
                writeTextLine(
                    textLine,
                    width.toFloat() / 2f - textLine.width / 2f,
                    height.toFloat() / 2f + fontSize * 5,
                )
            }
            it.flush()
            it.makeImageSnapshot()
        }

}

class SkikoAccessoryImageGenerator(private val userCache: UserCache) : SkikoImageGenerator(), ImageHelper {

    override suspend fun generate(): ByteArray {
        val image = DrawImageApiAdpater.getSkikoBackground(userCache.customBackgroundFile).makeImageSnapshot()
        val width = (image.width - image.width * 0.4f).toInt()
        return storeImage(
            image,
            Accessory.convert(StoreApiHelper.queryStoreFront(userCache)).map {
                singleSkinImage(width, width / 2, it)
            }
        )
    }

    private suspend fun singleSkinImage(width: Int, height: Int, data: AccessoryImageData): Image =
        Surface.makeRaster(ImageInfo.makeN32Premul(width, height)).afterClose {
            val textX: Float = width / 2f
            var heightY: Float = height / 2f
            val fontSize = height / 12f
            writeBackgroundColor(it, "#707070", 0.5f)
            if (data.itemUrl?.isNotEmpty() == true) {
                writeAutoSizeImage(client.get(data.itemUrl).readBytes(), it)
                heightY = height - (fontSize + fontSize / 5)
            }
            if (data.itemName?.isNotEmpty() == true) {
                val textLine = makeTextLine(data.itemName, fontSize)
                writeTextLine(
                    textLine,
                    textX - textLine.width / 2f,
                    heightY
                )
            }
            if (data.contractName?.isNotEmpty() == true) {
                val textLine = makeTextLine(data.contractName, fontSize)
                writeTextLine(
                    textLine,
                    textX - textLine.width / 2f,
                    height.toFloat() - fontSize / 5,
                )
            }
            it.flush()
            it.makeImageSnapshot()
        }

}