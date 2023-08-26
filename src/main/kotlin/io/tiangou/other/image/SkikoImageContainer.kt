package io.tiangou.other.image

import io.tiangou.Global
import org.jetbrains.skia.*

class SkikoImageContainer: ImageContainer {

    private lateinit var surface: Surface

    private fun byteToImage(bytes: ByteArray): Image = Image.makeFromEncoded(bytes)
    override val width: Int
        get() = surface.width
    override val height: Int
        get() = surface.height

    override fun init(width: Int, height: Int): ImageContainer {
        surface = Surface.makeRasterN32Premul(width, height)
        return this
    }

    override fun initBackground(bytes: ByteArray, wp: Int, hp: Int, alpha: Float): ImageContainer {
        val image = byteToImage(bytes)
        val maxSide = if (image.width > image.height) image.width else image.height
        var width: Int
        var height: Int
        if (maxSide == image.width) {
            width = image.height / hp * wp
            height = image.height
        } else {
            width = image.width
            height = image.width / wp * hp
        }
        if (width > image.width) {
            width = image.width
            height = image.width / wp * hp
        }
        if (height > image.height) {
            width = image.height / hp * wp
            height = image.height
        }
        init(width, height)
        surface.canvas.drawImageRect(
            image,
            Rect.makeXYWH((width - image.width) / 2f, (height - image.height) / 2f, image.width.toFloat(), image.height.toFloat()),
            Paint().setAlphaf(alpha)
        )
        return this
    }

    private fun rgbaConvert(rgba: String): Int {
        val colorString = rgba.removePrefix("#")
        return when (colorString.length) {
            6 -> Color.makeRGB(
                Integer.parseInt(colorString.substring(0, 2), 16),
                Integer.parseInt(colorString.substring(2, 4), 16),
                Integer.parseInt(colorString.substring(4, 6), 16)
            )

            8 -> Color.makeARGB(
                Integer.parseInt(colorString.substring(6, 8), 16),
                Integer.parseInt(colorString.substring(0, 2), 16),
                Integer.parseInt(colorString.substring(2, 4), 16),
                Integer.parseInt(colorString.substring(4, 6), 16)
            )

            else -> throw IllegalArgumentException("str is not RGB or RGBA format")
        }
    }

    override fun paddingBackgroundColor(rgba: String, alpha: Float?): ImageContainer {
        surface.canvas.drawRect(
            Rect.makeXYWH(0f, 0f, surface.width.toFloat(), surface.height.toFloat()),
            Paint().apply {
                color = rgbaConvert(rgba)
                mode = PaintMode.FILL
                if (alpha != null) {
                    setAlphaf(alpha)
                }
            }
        )
        return this
    }

    override fun drawImage(bytes: ByteArray, imageWidth: Int, imageHeight: Int, left: Int, top: Int, alpha: Float): ImageContainer {
        val image = byteToImage(bytes)
        surface.canvas.drawImageRect(
            image,
            Rect.makeXYWH(left.toFloat(), top.toFloat(), image.width.toFloat(), image.height.toFloat()),
            Paint().setAlphaf(alpha)
        )
        return this
    }

    override fun drawAutoSizeImage(bytes: ByteArray, reserve: Float, alpha: Float): ImageContainer {
        val image = byteToImage(bytes)
        var zoom = 1f / (image.width.toFloat() / surface.width.toFloat())
        var imageWidth = image.width * zoom
        var imageHeight = image.height * zoom
        val drawWidth = surface.width - surface.width * reserve
        val drawHeight = surface.height - surface.height * reserve
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
        surface.canvas.drawImageRect(
            image,
            Rect.makeXYWH(
                (surface.width - imageWidth) / 2,
                (surface.height - imageHeight) / 2,
                imageWidth,
                imageHeight
            ),
            Paint().setAlphaf(alpha)
        )
        return this
    }

    override fun drawHorizonAlignText(text: String, fontSize: Int, top: Int): ImageContainer {
        val textLine = TextLine.make(text, customerFont.makeWithSize(fontSize.toFloat()))
        surface.canvas.drawTextLine(textLine, surface.width / 2 - textLine.width / 2, top.toFloat(),
            Paint().apply { color = rgbaConvert(fontColor) }
        )
        return this
    }

    override fun generate(): ByteArray {
        surface.flushAndSubmit()
        val bytes = surface.makeImageSnapshot().encodeToData()!!.bytes
        surface.close()
        return bytes
    }

    companion object {

        private val customerFont: Font = Font(
            Global.drawImageConfig.font.reference.getResourceBytes()
                ?.let { Data.makeFromBytes(it) }
                ?.let { Typeface.makeFromData(it) }
        )

        private val fontColor = Global.drawImageConfig.font.color
    }
}