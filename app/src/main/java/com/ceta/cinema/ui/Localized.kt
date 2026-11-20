package com.ceta.cinema.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** A context that still IS the activity's (so sign-in and Custom Tabs work) but reads its strings in another language. */
private class LocalizedContext(base: Context, private val localized: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = localized
}

/** The Activity behind any context, however many wrappers (such as [LocalizedContext]) sit in between. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** Shows [content] in [language] ("vi" or "en") by swapping the resources the composition reads; no activity restart. */
@Composable
fun Localized(language: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val context = remember(base, language) {
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        LocalizedContext(base, base.createConfigurationContext(config).resources)
    }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration, content = content)
}

/**
 * A dialog or bottom sheet opens its own window whose composition resets LocalContext to the plain activity, so its strings would
 * come back in the phone's language. Capture [LocalContext] outside the window and wrap the window's content in this.
 */
@Composable
fun KeepLocale(context: Context, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration, content = content)
}
