package com.ceta.cinema.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Shows [content] in [language] ("vi" or "en") by swapping the resources the composition reads; no activity restart. */
@Composable
fun Localized(language: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val context = remember(base, language) {
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        base.createConfigurationContext(config)
    }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration, content = content)
}
