package com.kashif.cameraK.controller

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.awt.image.VolatileImage
import javax.swing.JPanel


class ImagePanel : JPanel(true) {
    private var volatileImage: VolatileImage? = null
    var currentImage: BufferedImage? = null
    private var renderCount = 0
    private var lastRenderTime = System.currentTimeMillis()

    init {
        background = Color(0, 0, 0, 0)
        isOpaque = false


        enableEvents(0L)
        isEnabled = false
    }


    override fun contains(x: Int, y: Int): Boolean = false

    fun updateImage(image: BufferedImage?) {
        currentImage = image
        renderCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRenderTime >= 1000) {
            println("Render FPS: $renderCount")
            renderCount = 0
            lastRenderTime = currentTime
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (currentImage == null) return

        val gc = graphicsConfiguration
        var valid = true

        if (volatileImage == null ||
            volatileImage?.width != width ||
            volatileImage?.height != height) {
            volatileImage = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT)
        }

        do {
            if (volatileImage?.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                volatileImage = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT)
            }

            volatileImage?.createGraphics()?.let { volatileG ->
                volatileG.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
                volatileG.composite = AlphaComposite.SrcOver
                volatileG.drawImage(currentImage, 0, 0, width, height, null)
                volatileG.dispose()
            }

            g.drawImage(volatileImage, 0, 0, null)

            valid = volatileImage?.contentsLost() != true
        } while (!valid)
    }
}
