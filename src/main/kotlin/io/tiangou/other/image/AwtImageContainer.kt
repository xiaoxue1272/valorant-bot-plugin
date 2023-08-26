package io.tiangou.other.image

import io.tiangou.Global
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class AwtImageContainer: ImageContainer {

    private lateinit var bufferedImage: BufferedImage

    private lateinit var graphics2D: Graphics2D

    override val width: Int
        get() = bufferedImage.width
    override val height: Int
        get() = bufferedImage.height

    private fun byteToImage(bytes: ByteArray): BufferedImage = ByteArrayInputStream(bytes).use { ImageIO.read(it) }

    override fun init(width: Int, height: Int): ImageContainer {
        bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        graphics2D = bufferedImage.createGraphics()
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        return this
    }

    override fun initBackground(bytes: ByteArray, wp: Int, hp: Int, alpha: Float): ImageContainer {
        byteToImage(bytes).apply {
            val maxSide = if (width > height) width else height
            var areaWidth: Int
            var areaHeight: Int
            if (maxSide == width) {
                areaWidth = height / hp * wp
                areaHeight = height
            } else {
                areaWidth = width
                areaHeight = width / wp * hp
            }
            if (areaWidth > width) {
                areaWidth = width
                areaHeight = width / wp * hp
            }
            if (areaHeight > height) {
                areaWidth = height / hp * wp
                areaHeight = height
            }
            bufferedImage = getSubimage(
                (width - areaWidth) / 2,
                (height - areaHeight) / 2,
                areaWidth,
                areaHeight
            ).setAlpha(alpha)
            graphics2D = bufferedImage.createGraphics()
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        }
        return this
    }

    private fun BufferedImage.setAlpha(alphaPercent: Float = 1f): BufferedImage {
        val resultImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .createGraphics().let {
                it.deviceConfiguration
                    .createCompatibleImage(width, height, Transparency.TRANSLUCENT)
                    .apply { it.dispose() }
            }

        for (x in resultImage.minX until resultImage.width) {
            for (y in resultImage.minY until resultImage.height) {
                var rgb = resultImage.getRGB(x, y)
                rgb = (alphaPercent * 255).toInt() shl 24 or (rgb and 0x00ffffff)
                resultImage.setRGB(x, y, rgb)
            }
        }
        resultImage.createGraphics().apply {
            composite = AlphaComposite.SrcIn
            drawImage(this@setAlpha, 0, 0, width, height, null)
        }.dispose()
        return resultImage
    }

    private fun rgbaConvert(rgba: String): Color {
        val colorString = rgba.removePrefix("#")
        return when (colorString.length) {
            6 -> Color(
                Integer.parseInt(colorString.substring(0, 2), 16),
                Integer.parseInt(colorString.substring(2, 4), 16),
                Integer.parseInt(colorString.substring(4, 6), 16)
            )
            8 -> Color(
                Integer.parseInt(colorString.substring(0, 2), 16),
                Integer.parseInt(colorString.substring(2, 4), 16),
                Integer.parseInt(colorString.substring(4, 6), 16),
                Integer.parseInt(colorString.substring(6, 8), 16),
            )
            else -> throw IllegalArgumentException("str is not RGB or RGBA format")
        }
    }

    override fun paddingBackgroundColor(rgba: String, alpha: Float?): ImageContainer {
        graphics2D.apply {
            val originColor = color
            color = rgbaConvert(rgba)
            fillRect(0, 0, bufferedImage.width, bufferedImage.height)
            color = originColor
        }
        if (alpha != null) {
            bufferedImage.setAlpha(alpha)
        }
        return this
    }

    override fun drawImage(
        bytes: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Int,
        top: Int,
        alpha: Float
    ): ImageContainer {
        graphics2D.drawImage(byteToImage(bytes).setAlpha(alpha), left, top, imageWidth, imageHeight, null)
        return this
    }

    override fun drawAutoSizeImage(bytes: ByteArray, reserve: Float, alpha: Float): ImageContainer {
        byteToImage(bytes).setAlpha(alpha).apply {
            var zoom = 1f / (width.toFloat() / bufferedImage.width.toFloat())
            var imageWidth = width * zoom
            var imageHeight = height * zoom
            val drawWidth = bufferedImage.width - bufferedImage.width * reserve
            val drawHeight = bufferedImage.height - bufferedImage.height * reserve
            if (imageWidth > drawWidth) {
                zoom = drawWidth / imageWidth
                imageWidth *= zoom
                imageHeight *= zoom
            }
            if (imageHeight > drawHeight) {
                zoom = drawHeight / imageHeight
                imageWidth *= zoom
                imageHeight *= zoom
            }
            graphics2D.drawImage(
                this,
                ((bufferedImage.width - imageWidth) / 2).toInt(),
                ((bufferedImage.height - imageHeight) / 2).toInt(),
                imageWidth.toInt(),
                imageHeight.toInt(),
                null
            )
        }
        return this
    }

    override fun drawHorizonAlignText(text: String, fontSize: Int, top: Int): ImageContainer {
        graphics2D.apply {
            paint = rgbaConvert(fontColor)
            font = (customerFont ?: font).deriveFont(fontSize.toFloat())
            drawString(text, bufferedImage.width / 2 - fontMetrics.stringWidth(text) / 2, top)
        }
        return this
    }

    override fun generate(): ByteArray {
        graphics2D.dispose()
        bufferedImage.flush()
        return ByteArrayOutputStream().use {
            ImageIO.write(bufferedImage, "png", it)
            it.toByteArray()
        }
    }

    companion object {

        private val customerFont: Font? =
            Global.drawImageConfig.font.reference.getResourceFile().let {
                when (it?.extension?.uppercase()) {
                    "TTF", "OTF" -> Font.createFont(Font.TRUETYPE_FONT, it)
                    "PFB" -> Font.createFont(Font.TYPE1_FONT, it)
                    else -> null
                }
            }

        private val fontColor = Global.drawImageConfig.font.color

    }
}