package com.ceta.cinema.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ceta.cinema.ui.KeepLocale
import com.ceta.cinema.ui.theme.Ceta

/**
 * The bottom sheet of the batch-3 design: surface background, 28 top corners, a 40x4 handle, a 55-60 % scrim. It keeps the app's
 * language inside its own window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CetaSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = Ceta.colors
    val appContext = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = c.sf, scrimColor = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(c.ln2)) },
    ) { KeepLocale(appContext) { androidx.compose.foundation.layout.Column { content() } } }
}
