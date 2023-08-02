@file:Suppress("unused")

package io.tiangou.utils

import io.tiangou.Global
import io.tiangou.ResourceReferenceType
import io.tiangou.other.image.awt.afterClose
import io.tiangou.other.image.awt.makeByImageAndProportion
import io.tiangou.other.image.awt.readImage
import io.tiangou.other.image.awt.setAlpha
import io.tiangou.other.image.skiko.afterClose
import io.tiangou.other.image.skiko.makeByImageAndProportion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File

object DrawImageApiAdpater {


    private val backgroundFile: File = with(Global.drawImageConfig.background) {
        reference.getResourceFile()
            ?: reference.apply {
                type = ResourceReferenceType.URL
                value = "https://game.gtimg.cn/images/val/wallpaper/Logo_Wallpapers/VALORANT_Logo_V.jpg"
                initResource()
            }.getResourceFile()!!
    }

    private val backgroundAlpha: Float = Global.drawImageConfig.background.alpha.toFloat()

    private val fontFile: File? = Global.drawImageConfig.font.reference.getResourceFile()

    private val fontAlpha: Float = Global.drawImageConfig.font.alpha.toFloat()

    private val wp: Int = 9

    private val hp: Int = 16

    internal suspend fun getAwtBackground(file: File?) =
        readImage(file?.readBytes() ?: backgroundFile.readBytes()).makeByImageAndProportion(wp, hp).setAlpha(backgroundAlpha)

    internal suspend inline fun <reified T> drawAwtOnBackground(file: File?, block: Graphics2D.(BufferedImage) -> T): T =
        getAwtBackground(file).afterClose { block(this, it) }

    internal suspend fun getSkikoBackground(file: File?) =
        runInterruptible(Dispatchers.IO) { Surface.makeByImageAndProportion(file?.readBytes() ?: backgroundFile.readBytes(), wp, hp, backgroundAlpha) }
    internal suspend inline fun <reified T> drawSkikoOnBackground(file: File?, block: Canvas.(Surface) -> T): T =
        getSkikoBackground(file).afterClose { block(this, it) }

    internal suspend fun getAwtFont(): java.awt.Font =
        runInterruptible(Dispatchers.IO) {
            when (fontFile?.extension?.uppercase()) {
                "TTF", "OTF" -> java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile)
                "PFB" -> java.awt.Font.createFont(java.awt.Font.TYPE1_FONT, fontFile)
                else -> GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts[0]
            }
        }

    internal suspend fun getSkikoFont(): org.jetbrains.skia.Font =
        runInterruptible(Dispatchers.IO) {
            org.jetbrains.skia.Font(fontFile?.readBytes()?.let { Data.makeFromBytes(it) }?.let { Typeface.makeFromData(it) })
        }


}