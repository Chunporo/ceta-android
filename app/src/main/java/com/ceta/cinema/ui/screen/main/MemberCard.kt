package com.ceta.cinema.ui.screen.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.QrMatrix
import com.ceta.cinema.domain.model.Loyalty
import com.ceta.cinema.domain.usecase.ExpiringPoints
import com.ceta.cinema.domain.usecase.MemberTier
import com.ceta.cinema.domain.usecase.MembershipPolicy
import com.ceta.cinema.domain.usecase.PointsEntry
import com.ceta.cinema.ui.component.CetaLogo
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface
import java.time.format.DateTimeFormatter

private val Amber = Color(0xFFF0B84C)      // oklch(0.85 0.13 75): the "points expire" warning
private val Gold = Color(0xFFE9C873)       // oklch(0.86 0.11 85): VIP name, logo and bar
private val GoldGlow = Color(0xFFD3AC49)   // oklch(0.8 0.12 85)
private val Earn = Color(0xFF3FA66B)       // oklch(0.6 0.15 150)

/** How a tier's card looks: its background, logo, name colour, progress bar and an optional inner border. */
private class TierLook(val drawBackground: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit, val logoTile: Color, val logoMark: Color, val name: Color, val bar: Brush, val border: Color?)

@Composable
private fun lookOf(tier: MemberTier): TierLook {
    val c = Ceta.colors
    return when (tier) {
        MemberTier.MEMBER -> TierLook(
            drawBackground = {
                drawRect(Brush.linearGradient(listOf(Color(0xFF656974), Color(0xFF3D4049)), Offset.Zero, Offset(size.width, size.height)))
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(size.width, 0f), size.width * 0.7f))
            },
            logoTile = Color.White, logoMark = Color(0xFF3D4049), name = Color.White, bar = SolidBrush(Color.White), border = null,
        )
        MemberTier.STAR -> TierLook(
            drawBackground = {
                drawRect(Color(0xFF1B1B21))
                drawRect(Brush.radialGradient(listOf(c.ac.copy(alpha = 0.55f), Color.Transparent), Offset(size.width, 0f), size.width * 0.75f))
                drawRect(Brush.radialGradient(listOf(Color(0xFF7440C4).copy(alpha = 0.55f), Color.Transparent), Offset(0f, size.height), size.width * 0.65f))
            },
            logoTile = c.ac, logoMark = Color.White, name = Color.White, bar = Brush.horizontalGradient(listOf(Color.White, c.ac)), border = null,
        )
        MemberTier.VIP -> TierLook(
            drawBackground = {
                drawRect(Brush.linearGradient(listOf(Color(0xFF1E1C18), Color(0xFF0E0E10)), Offset(size.width * 0.3f, 0f), Offset(size.width * 0.7f, size.height)))
                drawRect(Brush.radialGradient(listOf(GoldGlow.copy(alpha = 0.42f), Color.Transparent), Offset(size.width, 0f), size.width * 0.7f))
            },
            logoTile = Gold, logoMark = Color(0xFF1E1C18), name = Gold, bar = SolidBrush(Gold), border = Gold.copy(alpha = 0.5f),
        )
    }
}

private fun SolidBrush(color: Color): Brush = Brush.horizontalGradient(listOf(color, color))

@Composable
private fun tierName(tier: MemberTier): String = when (tier) { MemberTier.MEMBER -> "MEMBER"; MemberTier.STAR -> "STAR"; MemberTier.VIP -> "VIP" }

/**
 * The membership card (design item 15): logo and tier name, the "Member code" pill, the balance, expiring points and a three-milestone
 * progress bar with a line about the spend. Tapping it opens the code.
 */
@Composable
fun MemberCard(tier: MemberTier, balance: Long, expiring: ExpiringPoints?, spend: Long, year: Int, onClick: () -> Unit) {
    val look = lookOf(tier)
    val c = Ceta.colors
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape).drawBehind { look.drawBackground(this) }
            .then(if (look.border != null) Modifier.border(1.dp, look.border, shape) else Modifier).clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CetaLogo(26.dp, tile = look.logoTile, mark = look.logoMark)
                Spacer(Modifier.width(10.dp))
                Text("CETA ${tierName(tier)}", color = look.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
                Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.QrCode2, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.member_code), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.points_balance_label), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text(thousands(balance), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                }
                if (expiring != null) Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.Schedule, null, tint = Amber, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.points_expire, expiring.points.toInt(), expiring.on.format(DateTimeFormatter.ofPattern("dd/MM"))), color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            TierBar(tier, MembershipPolicy.progress(spend), look)
            Text(spendLine(tier, spend, year), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

/** MEMBER (0) - STAR (half) - VIP (end): the current milestone is a big white dot with a halo, passed ones smaller, the rest hollow. */
@Composable
private fun TierBar(tier: MemberTier, progress: Float, look: TierLook) {
    val reached = tier.ordinal
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
            val inner = maxWidth - 18.dp
            Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)))
            Box(Modifier.align(Alignment.CenterStart).width(9.dp + inner * progress).height(4.dp).clip(CircleShape).background(look.bar))
            listOf(0f, 0.5f, 1f).forEachIndexed { i, f ->
                val cur = i == reached
                val done = i < reached
                val d: Dp = if (cur) 18.dp else 12.dp
                Box(
                    Modifier.align(Alignment.CenterStart).offset(x = 9.dp + inner * f - d / 2).size(d).clip(CircleShape)
                        .then(if (cur) Modifier.border(4.dp, Color.White.copy(alpha = 0.22f), CircleShape) else Modifier)
                        .background(if (cur) Color.White else if (done) Color.White.copy(alpha = 0.85f) else Color(0xFF1B1B21))
                        .then(if (!cur && !done) Modifier.border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape) else Modifier),
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf(MemberTier.MEMBER to Alignment.CenterStart, MemberTier.STAR to Alignment.Center, MemberTier.VIP to Alignment.CenterEnd).forEach { (t, align) ->
                Text(tierName(t), color = Color.White.copy(alpha = if (t == tier) 1f else 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.align(align))
            }
        }
    }
}

@Composable
private fun spendLine(tier: MemberTier, spend: Long, year: Int): String {
    val next = MembershipPolicy.next(tier)
    return if (next == null) stringResource(R.string.spend_top, MoneyFormatter.format(MembershipPolicy.VIP_SPEND), year + 1)
    else stringResource(R.string.spend_next, year, MoneyFormatter.format(spend), MoneyFormatter.format(MembershipPolicy.missing(spend)), tierName(next))
}

/** The perks of one tier as small cards in a row you scroll sideways. */
@Composable
fun PerksRow(tier: MemberTier, onAllTiers: () -> Unit) {
    val c = Ceta.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.perks_title, tierName(tier).lowercase().replaceFirstChar { it.uppercase() }), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.all_tiers), color = c.ac, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAllTiers).padding(4.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            perksOf(tier).forEach { (icon, label) ->
                Column(Modifier.width(136.dp).cardSurface(RoundedCornerShape(20.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(c.acs), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = c.ac, modifier = Modifier.size(22.dp))
                    }
                    Text(stringResource(label), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
                }
            }
        }
    }
}

/** TODO(business): the perks per tier are the design's sample text, not a promise the backend keeps. */
private fun perksOf(tier: MemberTier): List<Pair<ImageVector, Int>> = when (tier) {
    MemberTier.MEMBER -> listOf(Icons.Rounded.LocalActivity to R.string.perk_m1, Icons.Rounded.Sell to R.string.perk_m2, Icons.Rounded.Redeem to R.string.perk_m3)
    MemberTier.STAR -> listOf(Icons.Rounded.LocalActivity to R.string.perk_s1, Icons.Rounded.Cake to R.string.perk_s2, Icons.Rounded.SwapHoriz to R.string.perk_s3, Icons.Rounded.Bolt to R.string.perk_s4)
    MemberTier.VIP -> listOf(Icons.Rounded.LocalActivity to R.string.perk_v1, Icons.Rounded.Cake to R.string.perk_v2, Icons.Rounded.Weekend to R.string.perk_v3, Icons.Rounded.SwapHoriz to R.string.perk_v4)
}

/** The last few points movements, with "See all" leading to the full order history. */
@Composable
fun PointsHistoryCard(entries: List<PointsEntry>, onSeeAll: () -> Unit) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.points_history), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.see_all), color = c.ac, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onSeeAll).padding(4.dp))
        }
        if (entries.isEmpty()) {
            Text(stringResource(R.string.points_history_empty), color = c.mu, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        } else entries.forEach { e ->
            HorizontalDivider(color = c.ln, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(c.sf2), contentAlignment = Alignment.Center) {
                    Icon(if (e.kind == PointsEntry.Kind.EARN) Icons.Rounded.ConfirmationNumber else Icons.Rounded.CurrencyExchange, null, tint = c.mu, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (e.kind == PointsEntry.Kind.EARN) R.string.points_log_booking else R.string.points_log_refund, e.movie.orEmpty()),
                        color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    e.date?.let { Text(it.format(DateTimeFormatter.ofPattern("dd/MM")), color = c.mu, fontSize = 12.sp) }
                }
                Text((if (e.points >= 0) "+ " else "− ") + kotlin.math.abs(e.points), color = if (e.points >= 0) Earn else c.ac, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** Contents of the "Member code" sheet: the QR on white, the code and how to use it. */
@Composable
fun MemberCodeContent(code: String, onClose: () -> Unit) {
    val c = Ceta.colors
    val payload = remember(code) { "MEM" + code.filter { it.isDigit() } }
    val matrix = remember(payload) { QrMatrix.of(payload) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.member_code), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth())
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(12.dp)) {
            Canvas(Modifier.size(175.dp)) {
                val n = matrix.size
                val cell = size.width / n
                for (y in 0 until n) for (x in 0 until n) if (matrix[y][x]) drawRect(Color(0xFF141416), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
        Text(code, color = c.tx, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Text(stringResource(R.string.member_hint), color = c.mu, fontSize = 14.sp, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        com.ceta.cinema.ui.component.OutlineButton(stringResource(R.string.close), onClose, pill = true)
    }
}

/** Contents of the "Card tiers" sheet: three mini cards with their condition and perks; the customer's own tier is outlined. */
@Composable
fun TiersContent(current: MemberTier) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.all_tiers), color = c.tx, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(R.string.tier_rule), color = c.mu, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(4.dp))
        MemberTier.entries.forEach { t ->
            val look = lookOf(t)
            val mine = t == current
            Box(Modifier.fillMaxWidth().then(if (mine) Modifier.border(2.dp, c.ac, RoundedCornerShape(22.dp)).padding(3.dp) else Modifier)) {
                Box(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(20.dp)).drawBehind { look.drawBackground(this) }.then(if (look.border != null) Modifier.border(1.dp, look.border, RoundedCornerShape(20.dp)) else Modifier)) {
                    Text("CETA ${tierName(t)}", color = look.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp, modifier = Modifier.padding(16.dp))
                    if (mine) Text(
                        stringResource(R.string.your_tier), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Text(
                        stringResource(when (t) { MemberTier.MEMBER -> R.string.tier_req_member; MemberTier.STAR -> R.string.tier_req_star; MemberTier.VIP -> R.string.tier_req_vip }),
                        color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    )
                }
            }
            Text(
                stringResource(when (t) { MemberTier.MEMBER -> R.string.tier_perks_member; MemberTier.STAR -> R.string.tier_perks_star; MemberTier.VIP -> R.string.tier_perks_vip }),
                color = c.mu, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

/** 2450 -> "2.450", the way the design writes points. */
private fun thousands(n: Long): String = "%,d".format(n).replace(',', '.')
