package com.ceta.cinema.ui.screen.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.AppNotification
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpen: (AppNotification) -> Unit, vm: NotificationsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.notifications), onBack) {
            if ((state as? UiState.Content)?.data?.unread ?: 0 > 0) {
                Text(stringResource(R.string.mark_all_read), color = c.ac, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 16.dp).clickable { vm.markAllRead() })
            }
        }
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> EmptyState(stringResource(R.string.notifications_empty), Modifier.padding(top = 48.dp))
            is UiState.Content -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.data.items, key = { it.id }) { n ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (n.read) c.sf else c.acs)
                            .clickable { vm.markRead(n.id); onOpen(n) }.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(n.title, color = c.tx, style = MaterialTheme.typography.titleSmall)
                            Text(n.body, color = c.mu, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!n.read) Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(c.ac))
                    }
                }
            }
        }
    }
}
