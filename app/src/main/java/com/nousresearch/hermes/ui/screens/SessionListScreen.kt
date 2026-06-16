package com.nousresearch.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nousresearch.hermes.data.model.HermesSession
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ConnectionViewModel
import com.nousresearch.hermes.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionListScreen(
    onSessionSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenCron: () -> Unit,
    onOpenChannels: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.state.collectAsState()
    val connectionState by connectionViewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showProfiles by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState.config) { sessionViewModel.refresh(connectionState.config) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                // Gradient header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0D0B2B),
                                    MaterialTheme.colorScheme.background,
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Logo pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                                contentAlignment = Alignment.Center,
                            ) { Text("⚡", fontSize = 18.sp) }
                            Column {
                                Text(
                                    "Hermes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Ink50,
                                )
                                connectionState.config?.let {
                                    Text(
                                        it.gatewayUrl.removePrefix("https://").removePrefix("http://").take(32),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Ink200,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        // Actions
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Ink200)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Cron & Tasks") }, leadingIcon = { Icon(Icons.Default.Schedule, null) }, onClick = { showMenu = false; onOpenCron() })
                            DropdownMenuItem(text = { Text("Channels") }, leadingIcon = { Icon(Icons.Default.Hub, null) }, onClick = { showMenu = false; onOpenChannels() })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Refresh sessions") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { showMenu = false; sessionViewModel.refresh(connectionState.config) })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Disconnect", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDisconnect() })
                        }
                    }
                }

                // Quick-action chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = onOpenCron,
                        label = { Text("Cron") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(10.dp),
                    )
                    AssistChip(
                        onClick = onOpenChannels,
                        label = { Text("Channels") },
                        leadingIcon = { Icon(Icons.Default.Hub, null, Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(10.dp),
                    )
                    Box {
                        AssistChip(
                            onClick = {
                                connectionViewModel.refreshProfiles()
                                showProfiles = true
                            },
                            label = { Text(connectionState.config?.profileName ?: "default") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null, Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(10.dp),
                        )
                        DropdownMenu(expanded = showProfiles, onDismissRequest = { showProfiles = false }) {
                            Text("Switch profile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            connectionState.profiles.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    leadingIcon = {
                                        if (p == connectionState.config?.profileName)
                                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                        else Spacer(Modifier.size(16.dp))
                                    },
                                    onClick = {
                                        connectionViewModel.changeProfile(p)
                                        showProfiles = false
                                        sessionViewModel.refresh(connectionState.config?.copy(profileName = p))
                                    },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Chat", fontWeight = FontWeight.SemiBold) },
                containerColor = Indigo500,
                contentColor   = Color.White,
                shape = RoundedCornerShape(16.dp),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                sessionState.isLoading && sessionState.sessions.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Indigo400)

                sessionState.sessions.isEmpty() ->
                    EmptyStateView(modifier = Modifier.align(Alignment.Center), onNewChat = onNewChat)

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sessionState.sessions, key = { it.sessionId }) { session ->
                        SessionCard(session = session, onClick = { onSessionSelected(session.sessionId) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: HermesSession, onClick: () -> Unit) {
    val initials = session.title
        ?.split(" ")
        ?.take(2)
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.joinToString("") ?: "#"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title ?: "Session ${session.sessionId.take(8)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatTimestamp(session.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink600, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyStateView(modifier: Modifier = Modifier, onNewChat: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Indigo700.copy(alpha = 0.4f), Ink800))),
            contentAlignment = Alignment.Center,
        ) {
            Text("💬", fontSize = 32.sp)
        }
        Text("No sessions yet", style = MaterialTheme.typography.titleMedium, color = Ink100)
        Text(
            "Start your first conversation with Hermes",
            style = MaterialTheme.typography.bodySmall,
            color = Ink200,
        )
        Button(
            onClick = onNewChat,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Color.White),
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Start chat")
        }
    }
}

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(ts))
