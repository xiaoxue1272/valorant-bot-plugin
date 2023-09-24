package io.tiangou.other.image

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.*
import io.tiangou.api.StoreApiHelper
import io.tiangou.other.http.client
import io.tiangou.repository.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible


object ImageGenerator {

    private val wp: Int = 9

    private val hp: Int = 16

    suspend fun getCacheOrGenerate(
        userCache: UserCache,
        type: GenerateImageType,
        block: suspend ImageGenerator.(GenerateImageType) -> ByteArray
    ): ByteArray = userCache.synchronous {
        val cache = generateImages.getOrDefault(type, CacheFactory.create())
        cache.get()?.takeIf { it.isNotEmpty() } ?: block(this@ImageGenerator, type).apply { cache.put(this) }
    }

    private fun createImageContainer(): ImageContainer = when (Global.drawImageConfig.api) {
        DrawImageApiEnum.SKIKO -> SkikoImageContainer()
        DrawImageApiEnum.AWT -> AwtImageContainer()
    }

    suspend fun storeImage(userCache: UserCache, type: GenerateImageType): ByteArray {
        val storeFront = StoreApiHelper.queryStoreFront(userCache)
        return createImageContainer().let {
            val backgroundBytes = userCache.customBackgroundFile?.readBytes()
                ?: Global.drawImageConfig.background.reference.getResourceBytes()!!
            it.initBackground(backgroundBytes, wp, hp)
            val width = (it.width - it.width * 0.4f).toInt()
            val height = width / 2
            val containers = when (type) {
                GenerateImageType.SKINS_PANEL_LAYOUT ->
                    SkinsPanelLayout.convert(storeFront).map { data ->
                        skinsPanelLayoutImage(width, height, data)
                    }

                GenerateImageType.ACCESSORY_STORE ->
                    AccessoryStore.convert(storeFront).map { data ->
                        accessoryStoreImage(width, height, data)
                    }
            }
            runInterruptible(Dispatchers.IO) {
                containers.forEachIndexed { index, container ->
                    val lr = it.width * 0.4f
                    val imageWidth = it.width - lr
                    val imageHeight = container.height * (imageWidth / container.width)
                    val top =
                        (it.height - imageHeight * containers.size - it.height * 0.05f * (containers.size - 1)) / 2
                    it.drawImage(
                        container.generate(),
                        imageWidth.toInt(),
                        imageHeight.toInt(),
                        (lr / 2f).toInt(),
                        (top + (imageHeight + it.height * 0.05f) * index).toInt()
                    )
                }
                it.generate()
            }
        }
    }


    private suspend fun skinsPanelLayoutImage(width: Int, height: Int, data: SkinImageData): ImageContainer =
        createImageContainer().apply {
            init(width, height)
            data.backgroundColor?.takeIf { it.isNotEmpty() }?.let { paddingBackgroundColor(it) }
            data.contentTiersUrl?.takeIf { it.isNotEmpty() }?.let {
                val contentTireSize = height / 6
                drawImage(
                    client.get(it).readBytes(),
                    contentTireSize,
                    contentTireSize,
                    width - contentTireSize
                )
            }
            data.themeUrl?.takeIf { it.isNotEmpty() }?.let {
                drawAutoSizeImage(client.get(it).readBytes(), alpha = 0.5f)
            }
            data.skinUrl?.takeIf { it.isNotEmpty() }?.let {
                drawAutoSizeImage(client.get(it).readBytes(), 0.3f)
            }
            data.skinName?.takeIf { it.isNotEmpty() }?.let {
                val fontSize = height / 12
                drawHorizonAlignText(it, fontSize, height / 2 + fontSize * 5)
            }
        }

    private suspend fun accessoryStoreImage(width: Int, height: Int, data: AccessoryImageData): ImageContainer =
        createImageContainer().apply {
            init(width, height)
            var heightY = height / 2
            val fontSize = height / 12
            paddingBackgroundColor("#707070", 0.5f)
            data.itemUrl?.takeIf { it.isNotEmpty() }?.let {
                drawAutoSizeImage(client.get(it).readBytes(), 0.4f)
                heightY = height - (fontSize + fontSize / 5)
            }
            data.itemName?.takeIf { it.isNotEmpty() }?.let { drawHorizonAlignText(it, fontSize, heightY) }
            data.contractName?.takeIf { it.isNotEmpty() }
                ?.let { drawHorizonAlignText(it, fontSize, height - fontSize / 5) }
        }

}