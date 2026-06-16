package com.nousresearch.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nousresearch.hermes.data.model.ChannelInfo
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ChannelManagerViewModel
import com.nousresearch.hermes.viewmodel.ConnectionViewModel

private val platformColors = mapOf(
    "telegram"  to Color(0xFF2AABEE),
    "discord"   to Color(0xFF5865F2),
    "slack"     to Color(0xFF4A154B),
    "matrix"    to Color(0xFF0DBD8B),
    "whatsapp"  to Color(0xFF25D366),
    "signal"    to Color(0xFF2C6BED),
    "email"     to Amber400,
    "sms"       to Green400,
)

@Composable
fun ChannelManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChannelManagerViewModel = hiltViewModel(),
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val connectionState by connectionViewModel.state.collectAsState()

    LaunchedEffect(connectionState.config) { viewModel.refresh(connectionState.config) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0D0B2B), MaterialTheme.colorScheme.background)))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink100)
                        }
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Hub, null, Modifier.size(18.dp), tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Channels", style = MaterialTheme.typography.titleMedium, color = Ink50)
                            Text("Connected platforms", style = MaterialTheme.typography.labelSmall, color = Ink200)
                        }
                        IconButton(onClick = { viewModel.refresh(connectionState.config) }) {
                            Icon(Icons.Default.Refresh, null, tint = Ink200)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.error != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp),
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = Amber400)
                    Spacer(Modifier.width(8.dp))
                    Text("Gateway channel directory API coming soon", style = MaterialTheme.typography.bodySmall, color = Ink200)
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Indigo400, trackColor = Ink700)

            if (state.channels.isEmpty() && !state.isLoading) {
                ChannelEmptyState(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 80.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.channels, key = { it.id + (it.threadId ?: "") }) { channel ->
                        ChannelCard(channel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(channel: ChannelInfo) {
    val accentColor = platformColors[channel.platform.lowercase()] ?: Indigo400
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Platform badge
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(channel.platform.take(2).uppercase(), color = accentColor, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(channel.name ?: channel.id, style = MaterialTheme.typography.titleSmall, color = Ink50, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (channel.isHome) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Indigo500.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("home", style = MaterialTheme.typography.labelSmall, color = Indigo400)
                    }
                }
            }
            Text(channel.id + (channel.threadId?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = Ink200, maxLines = 1, overflow = TextOverflow.Ellipsis)
            channel.lastSeenAt?.let { Text("Last seen: $it", style = MaterialTheme.typography.labelSmall, color = Ink600) }
        }
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
    }
}

@Composable
private fun ChannelEmptyState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Indigo700.copy(alpha = 0.4f), Ink800))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Hub, null, Modifier.size(32.dp), tint = Indigo400) }
        Text("No channels yet", style = MaterialTheme.typography.titleMedium, color = Ink100)
        Text("Connected platforms will appear here", style = MaterialTheme.typography.bodySmall, color = Ink200)
    }
}
