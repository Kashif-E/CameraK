package com.kashif.cameraK.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper


fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }

    return null
}