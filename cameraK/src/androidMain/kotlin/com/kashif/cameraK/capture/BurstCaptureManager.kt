package com.kashif.cameraK.capture

import com.kashif.cameraK.utils.MemoryManager
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

/**
 * Manager for handling burst mode captures efficiently on Android
 * Optimizes for rapid sequential photo captures with minimal delay
 * Implements throttling and quality adaptation based on system load
 */
class BurstCaptureManager {
    private val capturesInProgress = AtomicInteger(0)
    private val pendingCaptures = AtomicInteger(0)
    
    private val maxParallelCaptures = 2
    private val maxTotalCaptures = 8
    private val minCaptureIntervalMs = 300L
    
    private var lastCaptureTime = atomic(0L)
    
    private val processingExecutor = Executors.newFixedThreadPool(2)
    private val burstModeActive = atomic(false)
    private val lock = ReentrantLock()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    private val captureQuality = atomic(95)
    private val pendingCaptureQueue = ConcurrentLinkedQueue<CaptureRequest>()
    
    init {

        startQueueConsumer()
    }
    
    private data class CaptureRequest(
        val captureFunction: () -> Unit,
        val onComplete: () -> Unit
    )
    
    /**
     * Request a capture
     * @param captureFunction Function to call to initiate capture
     * @param onComplete Callback when capture is complete
     * @return true if capture was initiated or queued, false if rejected
     */
    fun requestCapture(
        captureFunction: () -> Unit,
        onComplete: () -> Unit
    ): Boolean {
        lock.withLock {
            val totalPending = pendingCaptures.get() + capturesInProgress.get()
            
            if (totalPending >= maxTotalCaptures) {
                return false
            }
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastCapture = currentTime - lastCaptureTime.value
            

            pendingCaptures.incrementAndGet()
            pendingCaptureQueue.add(CaptureRequest(captureFunction, onComplete))
            

            if (pendingCaptures.get() > 2) {
                burstModeActive.value = true
                updateCaptureQuality()
            }
            

            lastCaptureTime.value = currentTime
            
            return true
        }
    }
    
    /**
     * Start the queue consumer to process pending captures
     */
    private fun startQueueConsumer() {
        coroutineScope.launch {
            while (true) {
                processPendingCaptures()
                delay(50)
            }
        }
    }
    
    /**
     * Process any pending captures based on current state
     */
    private fun processPendingCaptures() {

        if (capturesInProgress.get() >= maxParallelCaptures) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastStart = currentTime - lastCaptureTime.value
        

        if (timeSinceLastStart < minCaptureIntervalMs && capturesInProgress.get() > 0) {
            return
        }
        

        val request = pendingCaptureQueue.poll() ?: return
        

        capturesInProgress.incrementAndGet()
        lastCaptureTime.value = currentTime
        

        processingExecutor.execute {
            try {

                MemoryManager.updateMemoryStatus()
                

                if (MemoryManager.isUnderMemoryPressure()) {
                    MemoryManager.clearBufferPools()
                }
                

                request.captureFunction()
            } finally {
                completeCapture(request.onComplete)
            }
        }
    }
    
    /**
     * Complete a capture and process next in queue
     */
    private fun completeCapture(onComplete: () -> Unit) {
        capturesInProgress.decrementAndGet()
        

        val remaining = pendingCaptures.decrementAndGet()
        

        if (remaining <= 0) {
            pendingCaptures.set(0)
            burstModeActive.value = false
            captureQuality.value = 95
        }
        

        onComplete()
    }
    
    /**
     * Update quality settings based on current state
     */
    private fun updateCaptureQuality() {
        val currentPending = pendingCaptures.get()
        val currentActive = capturesInProgress.get()
        val total = currentPending + currentActive
        
        captureQuality.value = when {
            MemoryManager.isUnderMemoryPressure() -> 65
            total > 5 -> 70
            total > 3 -> 80
            burstModeActive.value -> 85
            else -> 95
        }
    }
    
    /**
     * Check if burst mode is currently active
     */
    fun isBurstModeActive(): Boolean = burstModeActive.value
    
    /**
     * Get optimal quality setting based on current capture state and memory conditions
     */
    fun getOptimalQuality(): Int {
        updateCaptureQuality()
        return captureQuality.value
    }
    
    /**
     * Reset all capture state
     */
    fun reset() {
        lock.withLock {
            pendingCaptureQueue.clear()
            capturesInProgress.set(0)
            pendingCaptures.set(0)
            burstModeActive.value = false
            lastCaptureTime.value = 0
            captureQuality.value = 95
        }
    }
    
    /**
     * Clean up resources when no longer needed
     */
    fun shutdown() {
        processingExecutor.shutdown()
    }
}