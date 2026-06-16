package com.nousresearch.hermes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nousresearch.hermes.data.model.ChatMessage
import com.nousresearch.hermes.data.model.MessageRole
import com.nousresearch.hermes.data.model.ToolStatus
import com.nousresearch.hermes.ui.theme.*

@Composable
fun MessageBubble(message: ChatMessage) {
    when (message.role) {
        MessageRole.USER        -> UserBubble(message)
        MessageRole.ASSISTANT   -> AssistantBubble(message)
        MessageRole.TOOL_CALL   -> ToolCallRow(message)
        MessageRole.TOOL_RESULT -> Unit
        MessageRole.SYSTEM      -> Unit
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(Brush.linearGradient(listOf(Indigo500, Indigo700)))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(text = message.content, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 48.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp, bottom = 2.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo400, Indigo600))),
            contentAlignment = Alignment.Center,
        ) { Text("H", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (message.content.isNotEmpty()) {
                MarkdownText(text = message.content)
            }
            if (message.isStreaming) StreamCursor()
            if (message.content.length > 100 && !message.isStreaming) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { clipboard.setText(AnnotatedString(message.content)) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text("Copy", style = MaterialTheme.typography.labelSmall, color = Indigo400)
                }
            }
        }
    }
}

@Composable
private fun ToolCallRow(message: ChatMessage) {
    val (icon, tint) = when (message.toolStatus) {
        ToolStatus.SUCCESS          -> Icons.Default.Check to Green400
        ToolStatus.ERROR            -> Icons.Default.Close to Red400
        ToolStatus.WAITING_APPROVAL -> Icons.Default.Schedule to Amber400
        else                        -> Icons.Default.Terminal to Ink600
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(12.dp), tint = tint)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = message.toolName ?: "tool",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        if (message.toolStatus == ToolStatus.RUNNING) {
            Spacer(Modifier.width(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.weight(1f).height(2.dp).clip(RoundedCornerShape(1.dp)),
                color = Indigo400,
                trackColor = Ink700,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StreamCursor() {
    val t = rememberInfiniteTransition(label = "cursor")
    val alpha by t.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "cur_a")
    Box(
        modifier = Modifier.padding(top = 3.dp).size(width = 2.dp, height = 15.dp).background(Indigo400.copy(alpha = alpha))
    )
}

@Composable
fun MarkdownText(text: String) {
    val codeRe   = Regex("```[\\w]*\\n?([\\s\\S]*?)```")
    val inlineRe = Regex("`([^`]+)`")
    val boldRe   = Regex("\\*\\*([^*]+)\\*\\*")

    val parts      = codeRe.split(text)
    val codeBlocks = codeRe.findAll(text).map { it.groupValues[1].trimEnd('\n') }.toList()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEachIndexed { i, prose ->
            if (prose.isNotBlank()) {
                val annotated = buildAnnotatedString {
                    var cursor = 0
                    val matches = buildList {
                        inlineRe.findAll(prose).forEach { add(it) }
                        boldRe.findAll(prose).forEach { add(it) }
                    }.sortedBy { it.range.first }
                    matches.forEach { match ->
                        if (match.range.first > cursor) append(prose.substring(cursor, match.range.first))
                        when {
                            match.value.startsWith("**") -> {
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                append(match.groupValues[1])
                                pop()
                            }
                            match.value.startsWith("`") -> {
                                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, background = Ink750.copy(alpha = 0.7f), color = Cyan400))
                                append(match.groupValues[1])
                                pop()
                            }
                        }
                        cursor = match.range.last + 1
                    }
                    if (cursor < prose.length) append(prose.substring(cursor))
                }
                Text(text = annotated, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
            codeBlocks.getOrNull(i)?.let { code ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).padding(12.dp),
                ) {
                    Text(text = code, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp, color = Cyan400)
                }
            }
        }
    }
}
