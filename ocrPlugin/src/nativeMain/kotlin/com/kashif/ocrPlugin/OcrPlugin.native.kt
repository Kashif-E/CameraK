package com.kashif.ocrPlugin

import androidx.compose.ui.graphics.ImageBitmap
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.utils.toByteArray
import com.kashif.cameraK.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoStabilizationModeStandard
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreImage.CIImage
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.NSArray
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.valueForKey
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual suspend fun extractTextFromBitmapImpl(bitmap: ImageBitmap): String =
    suspendCoroutine { continuation ->
        NSLog("Starting text extraction from bitmap")


        val imageData = bitmap.toByteArray()?.toNSData()
        if (imageData == null) {
            NSLog("Failed to convert bitmap to NSData")
            continuation.resume("")
            return@suspendCoroutine
        }

        NSLog("Created NSData from bitmap")


        val uiImage = UIImage.imageWithData(imageData)
        if (uiImage == null) {
            NSLog("Failed to create UIImage from data")
            continuation.resume("")
            return@suspendCoroutine
        }

        NSLog("Created UIImage successfully")


        val request = VNRecognizeTextRequest { request, error ->
            if (error != null) {
                NSLog("Vision request error: ${error.localizedDescription}")
                continuation.resume("")
                return@VNRecognizeTextRequest
            }

            NSLog("Vision request completed")

            val results = request?.results as? NSArray
            if (results == null) {
                NSLog("No results from Vision request")
                continuation.resume("")
                return@VNRecognizeTextRequest
            }

            NSLog("Found ${results.count} results")

            val recognizedText = buildString {
                for (i in 0 until results.count.toInt()) {
                    val observation =
                        results.objectAtIndex(i.toULong()) as? VNRecognizedTextObservation
                    observation?.let { obs ->
                        val candidatesArray = obs.topCandidates(1u)
                        NSLog("Processing candidate ${i + 1}, found ${candidatesArray.count()} candidates")

                        if (candidatesArray.isNotEmpty()) {
                            val text =
                                (candidatesArray.first() as? NSObject)?.valueForKey("string") as? String
                            if (!text.isNullOrBlank()) {
                                NSLog("Found text: $text")
                                if (isNotEmpty()) append("\n")
                                append(text)
                            }
                        }
                    }
                }
            }

            NSLog("Final extracted text: $recognizedText")
            continuation.resume(recognizedText.trim())
        }


        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.usesLanguageCorrection = true

        NSLog("Vision request configured, attempting to process image")


        try {
            val handler = VNImageRequestHandler(uiImage.CGImage!!, mapOf<Any?, String>())

            NSLog("Created Vision request handler")


            handler.performRequests(listOf(request), null)
            NSLog("Vision request submitted")
        } catch (e: Exception) {
            NSLog("Error performing Vision request: ${e.message}")
            continuation.resume("")
        }
    }

@OptIn(ExperimentalForeignApi::class)
class VideoDataDelegate(
    private val onText: (String) -> Unit
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    private var isProcessingFrame = false
    private val processingQueue = dispatch_queue_create(
        "com.kashif.ocrPlugin.processing",
        null
    )


    private var lastProcessedTime: Double = 0.0
    private val minimumProcessingInterval = 0.1

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        if (didOutputSampleBuffer == null) return

        val currentTime = NSDate.date().timeIntervalSince1970()
        if (currentTime - lastProcessedTime < minimumProcessingInterval) {
            return
        }


        CFRetain(didOutputSampleBuffer)

        dispatch_async(processingQueue) {
            processFrame(didOutputSampleBuffer)
            lastProcessedTime = NSDate.date().timeIntervalSince1970()
        }
    }

    private fun processFrame(sampleBuffer: CMSampleBufferRef) {
        try {

            isProcessingFrame = true

            val pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: run {
                cleanupAndRelease(sampleBuffer)
                return
            }

            val ciImage = CIImage.imageWithCVPixelBuffer(pixelBuffer) ?: run {
                cleanupAndRelease(sampleBuffer)
                return
            }

            val request = VNRecognizeTextRequest { vnRequest, error ->
                if (error != null) {
                    NSLog("Text recognition error: ${error.localizedDescription}")
                    cleanupAndRelease(sampleBuffer)
                    return@VNRecognizeTextRequest
                }

                handleRecognitionResults(vnRequest?.results as? NSArray, sampleBuffer)
            }.apply {
                recognitionLevel = VNRequestTextRecognitionLevelAccurate
                usesLanguageCorrection = false
                minimumTextHeight = 0.1f
                recognitionLanguages = listOf("en-US")
            }

            val handler = ciImage.pixelBuffer?.let {
                VNImageRequestHandler(it, emptyMap<Any?, String>())
            } ?: run {
                NSLog("Failed to create image request handler")
                cleanupAndRelease(sampleBuffer)
                return
            }

            handler?.performRequests(listOf(request), null)

        } catch (e: Exception) {
            NSLog("Error processing frame: ${e.message}")
            cleanupAndRelease(sampleBuffer)
        }
    }

    private fun handleRecognitionResults(resultsArray: NSArray?, sampleBuffer: CMSampleBufferRef) {
        if (resultsArray == null || resultsArray.count.toInt() == 0) {
            cleanupAndRelease(sampleBuffer)
            return
        }

        val recognizedStrings = mutableListOf<String>()

        for (i in 0 until resultsArray.count.toInt()) {
            val observation = resultsArray.objectAtIndex(i.toULong()) as? VNRecognizedTextObservation
            observation?.let {
                val topCandidates = it.topCandidates(1u) as NSArray
                if (topCandidates.count > 0uL) {
                    val candidate = topCandidates.objectAtIndex(0uL) as? VNRecognizedText
                    val confidence = candidate?.confidence ?: 0.0f


                    if (confidence > 0.3f) {
                        candidate?.string?.let { text ->
                            if (text.isNotBlank()) {
                                recognizedStrings.add(text)
                            }
                        }
                    }
                }
            }
        }

        val extractedText = recognizedStrings.joinToString("\n")
        if (extractedText.isNotBlank()) {
            dispatch_async(dispatch_get_main_queue()) {
                onText(extractedText.trim())
                cleanupAndRelease(sampleBuffer)
            }
        } else {
            cleanupAndRelease(sampleBuffer)
        }
    }

    private fun cleanupAndRelease(sampleBuffer: CMSampleBufferRef) {
        isProcessingFrame = false
        CFRelease(sampleBuffer)
    }
}
@OptIn(ExperimentalForeignApi::class)
actual fun startRecognition(
    cameraController: CameraController,
    onText: (String) -> Unit
) {
    NSLog("Starting real-time camera text recognition")

    val previewLayer = cameraController.getCameraPreviewLayer() ?: run {
        NSLog("Failed to get camera preview layer")
        return
    }

    val videoDataOutput = AVCaptureVideoDataOutput().apply {

        setVideoSettings(
            mapOf(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA
            )
        )


        setAlwaysDiscardsLateVideoFrames(true)


        setSampleBufferDelegate(
            VideoDataDelegate(onText),
            dispatch_queue_create("com.kashif.ocrPlugin.videoQueue", null)
        )
    }

    val captureSession = previewLayer.session
    if (captureSession?.canAddOutput(videoDataOutput) == true) {
        captureSession.addOutput(videoDataOutput)


        (videoDataOutput.connections.firstOrNull() as? AVCaptureConnection)?.let { connection ->
            if (connection.supportsVideoOrientation) {
                connection.setVideoOrientation(AVCaptureVideoOrientationPortrait)
            }
            if (connection.supportsVideoStabilization) {
                connection.setPreferredVideoStabilizationMode(
                    AVCaptureVideoStabilizationModeStandard
                )
            }
        }

        NSLog("Added video data output to capture session")
    } else {
        NSLog("Cannot add video data output to capture session")
        return
    }

    cameraController.startSession()
    NSLog("Camera session started")
}