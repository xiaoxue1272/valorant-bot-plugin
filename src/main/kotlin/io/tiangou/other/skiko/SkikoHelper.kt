package io.tiangou.other.skiko

import org.jetbrains.skia.*


inline fun <reified T> Surface.afterClose(block: Surface.() -> T): T {
    val result = block(this)
    close()
    return result
}

internal fun Canvas.writeAutoSizeImage(
    bytes: ByteArray,
    surface: Surface,
    reservePercent: Float = 0f,
    alphaPercent: Float = 1f
): Canvas = apply {
    val image = Image.makeFromEncoded(bytes)
    var zoom = 1f / (image.width.toFloat() / surface.width.toFloat())
    var imageWidth = image.width * zoom
    var imageHeight = image.height * zoom
    val drawWidth = surface.width - surface.width * reservePercent
    val drawHeight = surface.height - surface.height * reservePercent
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
    drawImageRect(
        image,
        Rect.makeXYWH(
            (surface.width - imageWidth) / 2,
            (surface.height - imageHeight) / 2,
            imageWidth,
            imageHeight
        ),
        Paint().setAlphaf(alphaPercent)
    )
}


internal fun Canvas.writeImageRect(
    bytes: ByteArray,
    width: Float,
    height: Float,
    left: Float = 0f,
    top: Float = 0f,
    alphaPercent: Float = 1f
): Canvas =
    writeImageRect(Image.makeFromEncoded(bytes), width, height, left, top, alphaPercent)

internal fun Canvas.writeImageRect(
    image: Image,
    width: Float,
    height: Float,
    left: Float = 0f,
    top: Float = 0f,
    alphaPercent: Float = 1f
): Canvas = apply {
    drawImageRect(image, Rect.makeXYWH(left, top, width, height), Paint().setAlphaf(alphaPercent))
}

internal fun Canvas.writeImageRect(image: Image, left: Float = 0f, top: Float = 0f, alpha: Float = 1f): Canvas = apply {
    drawImageRect(
        image,
        Rect.makeXYWH(left, top, image.width.toFloat(), image.height.toFloat()),
        Paint().setAlphaf(alpha)
    )
}

internal fun Canvas.writeBackgroundColor(surface: Surface, rgbaString: String, alphaPercent: Float? = null) = apply {
    drawRect(Rect.makeXYWH(0f, 0f, surface.width.toFloat(), surface.height.toFloat()), Paint().apply {
        color = rgbaConvert(rgbaString)
        mode = PaintMode.FILL
        if (alphaPercent != null) {
            setAlphaf(alphaPercent)
        }
    })
}

internal fun rgbaConvert(str: String): Int {
    str.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("str is illegal")
    val colorString = str.removePrefix("#")
    return Color.makeARGB(
        Integer.parseInt(colorString.substring(6, 8), 16),
        Integer.parseInt(colorString.substring(0, 2), 16),
        Integer.parseInt(colorString.substring(2, 4), 16),
        Integer.parseInt(colorString.substring(4, 6), 16)
    )
}

internal fun Surface.Companion.makeByImageAndProportion(bytes: ByteArray, wp: Int, hp: Int): Surface =
    makeByImageAndProportion(Image.makeFromEncoded(bytes), wp, hp)

internal fun Surface.Companion.makeByImageAndProportion(image: Image, wp: Int, hp: Int): Surface {
    return if (image.width > image.height) {
        makeRasterN32Premul((image.height * wp.toFloat() / hp.toFloat()).toInt(), image.height)
    } else if (image.width < image.height) {
        makeRasterN32Premul(image.width, (image.width * hp.toFloat() / wp.toFloat()).toInt())
    } else {
        if (wp > hp) {
            makeRasterN32Premul((image.height * wp.toFloat() / hp.toFloat()).toInt(), image.height)
        } else if (hp > wp) {
            makeRasterN32Premul(image.width, (image.width * hp.toFloat() / wp.toFloat()).toInt())
        } else {
            makeRasterN32Premul(image.width, image.height)
        }
    }
}