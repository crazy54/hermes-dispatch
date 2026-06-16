package com.nousresearch.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ConnectionViewModel

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    onOpenPairing: (() -> Unit)? = null,
    viewModel: ConnectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    var url     by remember { mutableStateOf("") }
    var token   by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf("default") }
    var tokenVisible by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(state.isConnected) { if (state.isConnected) onConnected() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0B2B), Color(0xFF0F0F1A), Color(0xFF060612)),
                )
            ),
    ) {
        // Glow blobs
        Box(modifier = Modifier.offset((-60).dp, (-60).dp).size(280.dp).background(Brush.radialGradient(listOf(Indigo700.copy(alpha = 0.5f), Color.Transparent))))
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(60.dp, 60.dp).size(240.dp).background(Brush.radialGradient(listOf(Indigo500.copy(alpha = 0.3f), Color.Transparent))))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo
            AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { -40 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                        contentAlignment = Alignment.Center,
                    ) { Text("⚡", fontSize = 36.sp) }
                    Spacer(Modifier.height(20.dp))
                    Text("Hermes Dispatch", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Ink50)
                    Spacer(Modifier.height(6.dp))
                    Text("Your personal AI agent", style = MaterialTheme.typography.bodyMedium, color = Ink200)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Form card
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Connect to Gateway", style = MaterialTheme.typography.titleMedium, color = Ink100)

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Gateway URL") },
                    placeholder = { Text("https://hermes.yourdomain.com", color = Ink600) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = hermesTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                )

                // Token field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("API Key  (API_SERVER_KEY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = hermesTextFieldColors(),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle", tint = Ink200)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    )

                    // "How do I get this?" expandable hint
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { showHelp = !showHelp }.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("How do I get an API key?", style = MaterialTheme.typography.labelMedium, color = Indigo400)
                        Icon(if (showHelp) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp), tint = Indigo400)
                    }

                    AnimatedVisibility(visible = showHelp) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink850.copy(alpha = 0.8f)).border(1.dp, Ink700, RoundedCornerShape(14.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("On your Hermes gateway server, run:", style = MaterialTheme.typography.bodySmall, color = Ink200)

                            // Command block
                            val cmd = "openssl rand -hex 32"
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ink900).padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Cyan400)
                                IconButton(onClick = { clipboard.setText(AnnotatedString(cmd)) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp), tint = Ink200)
                                }
                            }

                            Text("Then add it to your gateway's .env file:", style = MaterialTheme.typography.bodySmall, color = Ink200)

                            val envLine = "API_SERVER_KEY=<your-generated-key>"
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ink900).padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(envLine, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Cyan400)
                                IconButton(onClick = { clipboard.setText(AnnotatedString(envLine)) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp), tint = Ink200)
                                }
                            }

                            Text("Then restart the gateway:", style = MaterialTheme.typography.bodySmall, color = Ink200)

                            val restartCmd = "hermes gateway restart"
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ink900).padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(restartCmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Cyan400)
                                IconButton(onClick = { clipboard.setText(AnnotatedString(restartCmd)) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp), tint = Ink200)
                                }
                            }

                            Text(
                                "The API_SERVER_KEY is the Bearer token Hermes Dispatch uses to authenticate with your gateway.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink600,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = profile,
                    onValueChange = { profile = it },
                    label = { Text("Profile") },
                    placeholder = { Text("default", color = Ink600) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = hermesTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focus.clearFocus()
                            if (url.isNotBlank() && token.isNotBlank())
                                viewModel.connect(url.trim(), token.trim(), profile.trim().ifEmpty { "default" })
                        }
                    ),
                )

                AnimatedVisibility(visible = state.error != null) {
                    state.error?.let { err ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
                        ) {
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.connect(url.trim(), token.trim(), profile.trim().ifEmpty { "default" }) },
                    enabled = url.isNotBlank() && token.isNotBlank() && !state.isConnecting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    if (state.isConnecting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Connecting…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Connect", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                if (onOpenPairing != null) {
                    TextButton(onClick = onOpenPairing, modifier = Modifier.fillMaxWidth()) {
                        Text("Scan QR to pair instead", color = Indigo400, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun hermesTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = Indigo400,
    unfocusedBorderColor  = Ink700,
    focusedLabelColor     = Indigo400,
    unfocusedLabelColor   = Ink200,
    cursorColor           = Indigo400,
    focusedTextColor      = Ink50,
    unfocusedTextColor    = Ink50,
    focusedContainerColor   = Ink800.copy(alpha = 0.5f),
    unfocusedContainerColor = Ink800.copy(alpha = 0.3f),
)
