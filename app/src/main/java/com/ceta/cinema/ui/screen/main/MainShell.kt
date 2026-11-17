package com.ceta.cinema.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ceta.cinema.R
import com.ceta.cinema.ui.theme.Ceta

enum class Tab(val icon: ImageVector, val label: Int) {
    HOME(Icons.Rounded.Home, R.string.ds_nav_home),
    CINEMAS(Icons.Rounded.Theaters, R.string.ds_nav_cinemas),
    TICKETS(Icons.Rounded.ConfirmationNumber, R.string.ds_nav_tickets),
    PROMOS(Icons.Rounded.Sell, R.string.ds_nav_promos),
    ACCOUNT(Icons.Rounded.Person, R.string.ds_nav_account),
}

/** The five tabs of the design. Each tab's content is supplied by the caller so screens stay independent of the shell. */
@Composable
fun MainShell(requested: Tab? = null, onTabShown: () -> Unit = {}, content: @Composable (Tab) -> Unit) {
    val c = Ceta.colors
    var selected by rememberSaveable { mutableIntStateOf(Tab.HOME.ordinal) }
    androidx.compose.runtime.LaunchedEffect(requested) { requested?.let { selected = it.ordinal; onTabShown() } }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content(Tab.entries[selected]) }
        Row(
            Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Tab.entries.forEach { tab ->
                val on = tab.ordinal == selected
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) c.acs else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selected = tab.ordinal }.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, null, tint = if (on) c.ac else c.mu, modifier = Modifier.size(24.dp))
                    Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall, color = if (on) c.ac else c.mu)
                }
            }
        }
    }
}
