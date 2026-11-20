package com.ceta.cinema.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.ageColor
import com.ceta.cinema.R

/** [pill] is the batch-3 button: 56 tall with fully round ends. The older screens keep the 52 / 16 shape. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, pill: Boolean = false) {
    val c = Ceta.colors
    Button(
        onClick = onClick, enabled = enabled && !loading, modifier = modifier.fillMaxWidth().height(if (pill) 56.dp else 52.dp),
        shape = RoundedCornerShape(if (pill) 28.dp else 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.ac, contentColor = Color.White, disabledContainerColor = c.ln2, disabledContentColor = c.mu),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, pill: Boolean = false) {
    val c = Ceta.colors
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(if (pill) 56.dp else 52.dp), shape = RoundedCornerShape(if (pill) 28.dp else 16.dp),
        border = BorderStroke(if (pill) 1.5.dp else 1.dp, c.ln2), colors = ButtonDefaults.outlinedButtonColors(contentColor = c.tx),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The design's pill switcher (Now showing / Coming soon, Upcoming / Watched, VI / EN). */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.sf).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) c.ac else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) Color.White else c.mu, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, if (selected) c.ac else c.ln2, RoundedCornerShape(20.dp))
            .background(if (selected) c.acs else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) c.ac else c.tx, style = MaterialTheme.typography.labelMedium) }
}

@Composable
fun AgeBadge(age: String?, modifier: Modifier = Modifier) {
    if (age.isNullOrBlank()) return
    Text(
        age, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(ageColor(age)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** A poster: the image when there is one, otherwise the design's diagonal-stripe placeholder in a per-movie hue. */
@Composable
fun Poster(url: String?, seed: String, modifier: Modifier = Modifier) {
    val hue = (seed.hashCode().toUInt().toLong() % 360).toFloat()
    val placeholder: @Composable () -> Unit = {
        val a = Color.hsv(hue, 0.55f, 0.55f)
        val b = Color.hsv(hue, 0.55f, 0.50f)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(a)
            val step = 20.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(b, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Butt)
                x += step
            }
        }
    }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Ceta.colors.sf2)) {
        if (url.isNullOrBlank()) placeholder()
        else SubcomposeAsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            loading = { placeholder() }, error = { placeholder() })
    }
}

@Composable
fun CetaTopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val c = Ceta.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.tx, modifier = Modifier.size(22.dp))
            }
        } else Spacer(Modifier.width(8.dp))
        Text(title, color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val c = Ceta.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.tx, modifier = Modifier.weight(1f))
        if (action != null) Text(action, style = MaterialTheme.typography.labelMedium, color = c.ac, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ceta.colors.ac) }
}

/** Every screen has an error state with a retry; the text comes from the error code, never from the server. */
@Composable
fun ErrorBox(error: ApiError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(ErrorMapper.messageFor(error)), color = c.tx, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            OutlineButton(stringResource(R.string.retry), onRetry, Modifier.width(180.dp))
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier, cta: String? = null, onCta: () -> Unit = {}) {
    val c = Ceta.colors
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, color = c.mu, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (cta != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(cta, onCta, Modifier.width(200.dp))
        }
    }
}

@Composable
fun Dot(active: Boolean, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    Box(modifier.height(8.dp).width(if (active) 26.dp else 8.dp).clip(CircleShape).background(if (active) c.ac else c.ln2))
}
