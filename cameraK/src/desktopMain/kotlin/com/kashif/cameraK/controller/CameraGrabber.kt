package com.kashif.cameraK.controller


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.awt.image.BufferedImage


class CameraGrabber(
    private val frameChannel: Channel<BufferedImage>,
    private val errorHandler: (Throwable) -> Unit
) {
    private val converter = FrameConverter()
    private var grabber: FFmpegFrameGrabber? = null
    private var job: Job? = null

    fun grabCurrentFrame(): BufferedImage? {
        val frame = grabber?.grab()
        return if (frame?.image != null) {
            converter.convert(frame)
        } else {
            null
        }
    }
    fun start(coroutineScope: CoroutineScope) {
        if (job?.isActive == true) return

        grabber = createGrabber().apply {
            try {
                start()
            } catch (e: Exception) {
                errorHandler(e)
                return
            }
        }

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

    private fun createGrabber() = when {
        System.getProperty("os.name").lowercase().contains("mac") -> {
            FFmpegFrameGrabber("default").apply {
                format = "avfoundation"
            }
        }
        System.getProperty("os.name").lowercase().contains("windows") -> {
            FFmpegFrameGrabber("video=0").apply {
                format = "dshow"
            }
        }
        else -> {
            FFmpegFrameGrabber("/dev/video0").apply {
                format = "v4l2"
            }
        }
    }.apply {
        frameRate = 30.0
        imageWidth = 640
        imageHeight = 480
    }
}