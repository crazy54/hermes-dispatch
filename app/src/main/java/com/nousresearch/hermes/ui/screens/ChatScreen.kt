package com.nousresearch.hermes.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.nousresearch.hermes.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nousresearch.hermes.ui.components.ApprovalDialog
import com.nousresearch.hermes.ui.components.MessageBubble
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ChatViewModel
import com.nousresearch.hermes.viewmodel.ConnectionViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    chatVm: ChatViewModel = hiltViewModel(),
    connVm: ConnectionViewModel = hiltViewModel(),
) {
    val chatState by chatVm.state.collectAsState()
    val connState by connVm.state.collectAsState()
    val config = connState.config
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(sessionId) {
        if (config != null) {
            if (sessionId == "new") chatVm.startNewSession(config) else chatVm.loadSession(sessionId, config)
        }
    }
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty())
            scope.launch { listState.animateScrollToItem(chatState.messages.lastIndex) }
    }

    chatState.pendingApproval?.let { approval ->
        if (config != null) {
            ApprovalDialog(
                approval  = approval,
                onApprove = { chatVm.submitApproval(config, approval.approvalId, true) },
                onDeny    = { chatVm.submitApproval(config, approval.approvalId, false) },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0D0B2B), MaterialTheme.colorScheme.background))
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink100)
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_hermes_logo),
                                contentDescription = "Hermes",
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chatState.currentSessionId?.let { "Session ${it.take(8)}" } ?: "New Chat",
                                style = MaterialTheme.typography.titleSmall,
                                color = Ink50,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (chatState.isLoading) Text("Thinking…", style = MaterialTheme.typography.labelSmall, color = Indigo400)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(chatState.messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                }
                if (chatState.isLoading && chatState.messages.none { it.isStreaming }) {
                    item("loading") {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Start) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                ThinkingDots()
                            }
                        }
                    }
                }
            }

            // Error
            AnimatedVisibility(visible = chatState.error != null) {
                chatState.error?.let { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { chatVm.clearError() }) { Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer) }
                    }
                }
            }

            // Input
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text("Message Hermes…", color = Ink600, fontSize = 15.sp) },
                    minLines = 1,
                    maxLines = 6,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Indigo400.copy(alpha = 0.7f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                        cursorColor          = Indigo400,
                    ),
                )
                if (chatState.isLoading) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { chatVm.stopStreaming() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (input.isNotBlank() && config != null)
                                    Brush.linearGradient(listOf(Indigo500, Indigo700))
                                else
                                    Brush.linearGradient(listOf(Ink700, Ink700))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = {
                                val text = input.trim()
                                if (text.isNotEmpty() && config != null) {
                                    input = ""
                                    chatVm.sendMessage(text, config)
                                }
                            },
                            enabled = input.isNotBlank() && config != null,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (input.isNotBlank()) Color.White else Ink600)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    val infinity = rememberInfiniteTransition(label = "dots")
    val offset by infinity.animateFloat(
        initialValue = 0f,
        targetValue  = 3f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "dots_phase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        (0..2).forEach { i ->
            val active = (offset.toInt() == i)
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) Indigo400 else Ink600),
            )
        }
    }
}
