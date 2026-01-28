package com.kashif.cameraK.utils

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

/**
 * Initializer for setting up application context on startup.
 * Used by AndroidX Startup library for automatic initialization.
 */
internal class AppContextInitializer : Initializer<Context> {
    /**
     * Creates and initializes the application context.
     *
     * @param context The application context.
     * @return The initialized application context.
     */
    override fun create(context: Context): Context {
        AppContext.setUp(context.applicationContext)
        return AppContext.get()
    }

    /**
     * Specifies this initializer has no dependencies.
     *
     * @return Empty list of initializer dependencies.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/**
 * Singleton holder for the application context.
 * Provides global access to the Android application context for camera operations.
 */
internal object AppContext {
    private lateinit var application: Application

    /**
     * Initializes the context with the application instance.
     *
     * @param context The application context to store.
     */
    fun setUp(context: Context) {
        application = context as Application
    }

    /**
     * Retrieves the stored application context.
     *
     * @return The application context.
     * @throws Exception If context has not been initialized.
     */
    fun get(): Context {
        if (!AppContext::application.isInitialized) {
            throw Exception("Context is not initialized.")
        }
        return application.applicationContext
    }
}
