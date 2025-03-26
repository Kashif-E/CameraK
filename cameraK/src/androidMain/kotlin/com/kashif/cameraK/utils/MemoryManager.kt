package com.kashif.cameraK.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.atomicfu.atomic
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manages memory resources for camera operations on Android
 * Monitors memory pressure and optimizes memory usage for image capture operations
 */
object MemoryManager {

    private const val MEMORY_PRESSURE_THRESHOLD = 0.8


    private val smallBufferLock = ReentrantLock()
    private val mediumBufferLock = ReentrantLock()
    private val largeBufferLock = ReentrantLock()


    private val memoryPressure = atomic(false)


    private var memoryUsage = atomic(0.0)


    private val smallBufferPool = mutableListOf<ByteArray>()
    private val mediumBufferPool = mutableListOf<ByteArray>()
    private val largeBufferPool = mutableListOf<ByteArray>()


    private var appContext: Context? = null

    /**
     * Initialize memory monitoring with application context
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        updateMemoryStatus()
    }

    /**
     * Update current memory status
     * This should be called periodically, especially before major memory operations
     */
    fun updateMemoryStatus() {
        val context = appContext ?: return
        
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemory = memoryInfo.availMem
        val totalMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memoryInfo.totalMem
        } else {
            Runtime.getRuntime().totalMemory()
        }
        
        if (totalMemory > 0) {
            val usedMemory = totalMemory - availableMemory
            val usage = usedMemory.toDouble() / totalMemory.toDouble()
            memoryUsage.value = usage
            

            if (usage > MEMORY_PRESSURE_THRESHOLD && !memoryPressure.value) {
                memoryPressure.value = true
                handleHighMemoryPressure()
            } else if (usage <= MEMORY_PRESSURE_THRESHOLD && memoryPressure.value) {
                memoryPressure.value = false
            }
        }
        

        if (memoryInfo.lowMemory && !memoryPressure.value) {
            memoryPressure.value = true
            handleHighMemoryPressure()
        }
    }

    /**
     * Handle high memory pressure situation
     */
    private fun handleHighMemoryPressure() {
        clearBufferPools()
        System.gc()
    }

    /**
     * Clear all buffer pools to free memory
     * Should be called when memory pressure is detected
     */
    fun clearBufferPools() {
        smallBufferLock.withLock {
            smallBufferPool.clear()
        }
        
        mediumBufferLock.withLock {
            mediumBufferPool.clear()
        }
        
        largeBufferLock.withLock {
            largeBufferPool.clear()
        }
    }


    /**
     * Check if memory is under pressure
     */
    fun isUnderMemoryPressure(): Boolean {
        return memoryPressure.value
    }

}