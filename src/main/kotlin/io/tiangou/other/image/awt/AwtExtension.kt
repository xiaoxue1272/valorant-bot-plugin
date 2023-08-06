package io.tiangou.other.image.awt

import io.tiangou.Global
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


private val customerFont: Font? =
    Global.drawImageConfig.font.reference.getResourceFile().let {
        when (it?.extension?.uppercase()) {
            "TTF", "OTF" -> Font.createFont(Font.TRUETYPE_FONT, it)
            "PFB" -> Font.createFont(Font.TYPE1_FONT, it)
            else -> null
        }
    }

inline fun <reified T> BufferedImage.afterClose(block: Graphics2D.(BufferedImage) -> T): T {
    val graphics2D = createGraphics()
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val result = block(graphics2D, this)
    graphics2D.dispose()
    return result
}

internal suspend fun readImage(bytes: ByteArray): BufferedImage {
    return runInterruptible(Dispatchers.IO) {
        ByteArrayInputStream(bytes).use {
            ImageIO.read(it)
        }
    }
}

internal suspend fun readBackgroundImage(bytes: ByteArray?): BufferedImage =
    readImage(bytes ?: Global.drawImageConfig.background.reference.getResourceBytes()!!)

internal suspend fun BufferedImage.writeImage(): ByteArray {
    return runInterruptible(Dispatchers.IO) {
        ByteArrayOutputStream().use {
            ImageIO.write(this, "png", it)
            it.toByteArray()
        }
    }
}

internal fun BufferedImage.makeByImageAndProportion(wp: Int, hp: Int): BufferedImage {
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
    return getSubimage(
        (width - areaWidth) / 2,
        (height - areaHeight) / 2,
        areaWidth,
        areaHeight
    ).setAlpha(Global.drawImageConfig.background.alpha.toFloat())
}

internal fun BufferedImage.setBackgroundColor(rgbaString: String? = null, alphaPercent: Float? = null): BufferedImage =
    apply {
        if (rgbaString?.isNotEmpty() == true) {
            createGraphics().apply {
                val originColor = color
                color =
                    if (alphaPercent == null) rgbaConvert(rgbaString, true) else rgbaConvert(rgbaString, alphaPercent)
                fillRect(0, 0, width, height)
                color = originColor
            }.dispose()
        }
    }

internal suspend fun Graphics.writeImageRect(
    bytes: ByteArray,
    width: Int,
    height: Int,
    left: Int = 0,
    top: Int = 0,
    alphaPercent: Float = 1f
): Graphics = apply {
    drawImage(
        readImage(bytes).setAlpha(alphaPercent),
        left,
        top,
        width,
        height,
        null
    )
}

internal fun Graphics.writeImageRect(
    image: BufferedImage,
    width: Int,
    height: Int,
    left: Int = 0,
    top: Int = 0,
    alphaPercent: Float = 1f
): Graphics = apply {
    drawImage(
        image.setAlpha(alphaPercent),
        left,
        top,
        width,
        height,
        null
    )
}

internal fun BufferedImage.setAlpha(alphaPercent: Float = 1f): BufferedImage {
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

internal suspend fun Graphics.writeAutoSizeImage(
    bytes: ByteArray,
    originImage: BufferedImage,
    reservePercent: Float = 0f,
    alphaPercent: Float = 1f
): Graphics = apply {
    val image = readImage(bytes).setAlpha(alphaPercent)
    var zoom = 1f / (image.width.toFloat() / originImage.width.toFloat())
    var imageWidth = image.width * zoom
    var imageHeight = image.height * zoom
    val drawWidth = originImage.width - originImage.width * reservePercent
    val drawHeight = originImage.height - originImage.height * reservePercent
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
    drawImage(
        image,
        ((originImage.width - imageWidth) / 2).toInt(),
        ((originImage.height - imageHeight) / 2).toInt(),
        imageWidth.toInt(),
        imageHeight.toInt(),
        null
    )
}

internal fun rgbaConvert(str: String, parseAlpha: Boolean = true): Color {
    str.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("str is empty")
    val colorString = str.removePrefix("#")

    return when (colorString.length) {
        6 -> Color(
            Integer.parseInt(colorString.substring(0, 2), 16),
            Integer.parseInt(colorString.substring(2, 4), 16),
            Integer.parseInt(colorString.substring(4, 6), 16)
        )

        8 -> {
            if (parseAlpha) {
                Color(
                    Integer.parseInt(colorString.substring(0, 2), 16),
                    Integer.parseInt(colorString.substring(2, 4), 16),
                    Integer.parseInt(colorString.substring(4, 6), 16),
                    Integer.parseInt(colorString.substring(6, 8), 16)
                )
            } else {
                Color(
                    Integer.parseInt(colorString.substring(0, 2), 16),
                    Integer.parseInt(colorString.substring(2, 4), 16),
                    Integer.parseInt(colorString.substring(4, 6), 16),
                )
            }
        }

        else -> throw IllegalArgumentException("str is not RGB or RGBA format")
    }
}

internal fun rgbaConvert(str: String, alphaPercent: Float = 1f): Color {
    str.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("str is empty")
    val colorString = str.removePrefix("#")

    return if (colorString.length >= 6) {
        Color(
            Integer.parseInt(colorString.substring(0, 2), 16),
            Integer.parseInt(colorString.substring(2, 4), 16),
            Integer.parseInt(colorString.substring(4, 6), 16),
            (255 * alphaPercent).toInt()
        )
    } else throw IllegalArgumentException("str is not RGB or RGBA format")
}

internal fun Graphics2D.writeHorizontallyAlignTextLine(str: String, width: Int, fontSize: Int, y: Int) {
    paint = rgbaConvert(Global.drawImageConfig.font.color, true)
    font = (customerFont ?: font).deriveFont(fontSize.toFloat())
    drawString(str, width / 2 - fontMetrics.stringWidth(str) / 2, y)
}