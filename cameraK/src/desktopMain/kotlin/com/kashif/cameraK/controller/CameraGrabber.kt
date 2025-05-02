package com.kashif.cameraK.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.VideoInputFrameGrabber
import java.awt.image.BufferedImage

class CameraGrabber(
    private val frameChannel: Channel<BufferedImage>,
    private val errorHandler: (Throwable) -> Unit
) {
    private val converter = FrameConverter()
    private var grabber: FrameGrabber? = null
    private var job: Job? = null

    private var isHorizontallyFlipped = false

    fun setHorizontalFlip(flip: Boolean) {
        this.isHorizontallyFlipped = flip
        converter.setHorizontalFlip(flip)
    }

    fun grabCurrentFrame(): BufferedImage? {
        val frame = grabber?.grab()
        return if (frame?.image != null) {
            converter.convert(frame)
        } else {
            null
        }
    }

    fun start(coroutineScope: CoroutineScope, customGrabber: FrameGrabber? = null) {
        if (job?.isActive == true) return

        grabber = (customGrabber ?: createGrabber()).apply {
            try {
                start()
            } catch (e: Exception) {
                errorHandler(e)
                return
            }
        }

        converter.setHorizontalFlip(isHorizontallyFlipped)

        job = coroutineScope.launch(Dispatchers.IO) {
            var frameCount = 0
            var lastFpsTime = System.currentTimeMillis()

            try {
                while (isActive) {
                    val frame = grabber?.grab()
                    if (frame?.image != null) {
                        converter.convert(frame)?.let { image ->
                            frameChannel.trySend(image)

                            frameCount++
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastFpsTime >= 1000) {
                                println("Camera FPS: $frameCount")
                                frameCount = 0
                                lastFpsTime = currentTime
                            }
                        }
                    }

                    delay(1)
                }
            } catch (e: Exception) {
                errorHandler(e)
            }
        }
    }

    fun stop() {
        job?.cancel()
        try {
            grabber?.stop()
            grabber?.release()
            converter.release()
        } catch (e: Exception) {
            errorHandler(e)
        }
    }

    private fun createGrabber() : FrameGrabber =
        when (getOperatingSystem()) {
            OperatingSystem.MAC -> {
                FFmpegFrameGrabber("default")
            }
            OperatingSystem.WINDOWS -> {
                VideoInputFrameGrabber(0)
            }
            OperatingSystem.LINUX -> {
                FFmpegFrameGrabber("/dev/video0")
            }
        }.apply {
            frameRate = 30.0
            imageWidth = 640
            imageHeight = 480
        }

    private fun getOperatingSystem(): OperatingSystem {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> OperatingSystem.MAC
            os.contains("windows") -> OperatingSystem.WINDOWS
            else -> OperatingSystem.LINUX
        }
    }
}

enum class OperatingSystem {
    WINDOWS,
    MAC,
    LINUX
}
