package com.nousresearch.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nousresearch.hermes.ui.theme.Indigo400
import com.nousresearch.hermes.ui.theme.Ink700

/**
 * Subtle glass-morphism card used throughout the app.
 * Translucent dark background with a thin gradient border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderAlpha: Float = 0.35f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Indigo400.copy(alpha = borderAlpha),
                        Ink700.copy(alpha = borderAlpha * 0.5f),
                    ),
                ),
                shape = shape,
            ),
        content = content,
    )
}

/** Thin horizontal rule that matches the outline color. */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
