package com.nousresearch.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nousresearch.hermes.data.model.ApprovalRequest
import com.nousresearch.hermes.data.model.RiskLevel
import com.nousresearch.hermes.ui.theme.*

@Composable
fun ApprovalDialog(
    approval: ApprovalRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    val riskColor = when (approval.riskLevel) {
        RiskLevel.CRITICAL, RiskLevel.HIGH -> Red400
        RiskLevel.MEDIUM                   -> Amber400
        RiskLevel.LOW                      -> Green400
    }
    val riskBg = riskColor.copy(alpha = 0.10f)

    AlertDialog(
        onDismissRequest = onDeny,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(riskBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (approval.riskLevel == RiskLevel.CRITICAL || approval.riskLevel == RiskLevel.HIGH)
                        Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = null,
                    tint = riskColor,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Approval Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(riskBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text  = approval.riskLevel.name,
                        color = riskColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = approval.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Command block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.Terminal, null, Modifier.size(14.dp), tint = Indigo400)
                        Text("Command", style = MaterialTheme.typography.labelSmall, color = Indigo400, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text  = "$ " + approval.command,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Cyan400,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text  = approval.cwd,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Approve", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDeny,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                border = androidx.compose.foundation.BorderStroke(1.dp, Red400.copy(alpha = 0.5f)),
            ) {
                Text("Deny", fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
