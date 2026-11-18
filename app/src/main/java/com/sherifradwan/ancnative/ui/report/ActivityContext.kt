package com.sherifradwan.ancnative.ui.report

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Walks the [ContextWrapper] chain to locate the hosting [Activity], or returns null if none exists.
 */
fun Context.findActivity(): Activity? {
    val visited = Collections.newSetFromMap(IdentityHashMap<Context, Boolean>())
    var current: Context? = this
    while (current != null && visited.add(current)) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
