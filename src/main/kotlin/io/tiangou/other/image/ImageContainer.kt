package io.tiangou.other.image

import io.tiangou.Global

interface ImageContainer {

    val width: Int

    val height: Int

    fun init(width: Int, height: Int): ImageContainer

    fun initBackground(
        bytes: ByteArray,
        wp: Int,
        hp: Int,
        alpha: Float = Global.drawImageConfig.background.alpha.toFloat()
    ): ImageContainer

    fun paddingBackgroundColor(rgba: String, alpha: Float? = null): ImageContainer

    fun drawImage(
        bytes: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Int = 0,
        top: Int = 0,
        alpha: Float = 1f
    ): ImageContainer

    fun drawAutoSizeImage(bytes: ByteArray, reserve: Float = 0f, alpha: Float = 1f): ImageContainer

    fun drawHorizonAlignText(text: String, fontSize: Int, top: Int): ImageContainer

    fun generate(): ByteArray

}