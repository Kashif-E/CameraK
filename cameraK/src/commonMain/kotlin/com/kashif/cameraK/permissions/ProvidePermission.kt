package com.kashif.cameraK.permissions


import androidx.compose.runtime.Composable

/**
 * Factory function to provide platform-specific [Permissions] implementation.
 */
@Composable
expect fun providePermissions(): Permissions