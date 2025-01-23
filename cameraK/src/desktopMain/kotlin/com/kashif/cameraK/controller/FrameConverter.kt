package com.kashif.cameraK.controller

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.GraphicsEnvironment
import java.awt.Transparency
import java.awt.image.BufferedImage

class FrameConverter {
    private val converter = Java2DFrameConverter()
    private var cachedImage: BufferedImage? = null
    private val graphicsConfig = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration

    fun convert(frame: Frame): BufferedImage? {
        return try {
            if (cachedImage == null ||
                cachedImage?.width != frame.imageWidth ||
                cachedImage?.height != frame.imageHeight) {
                cachedImage = graphicsConfig.createCompatibleImage(
                    frame.imageWidth,
                    frame.imageHeight,
                    Transparency.OPAQUE
                )
            }
            converter.convert( frame)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun release() {
        converter.close()
        cachedImage = null
    }
}