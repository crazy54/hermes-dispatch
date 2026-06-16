package com.nousresearch.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ConnectionViewModel

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    viewModel: ConnectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val focus = LocalFocusManager.current

    var url   by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf("default") }
    var tokenVisible by remember { mutableStateOf(false) }

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
        // Decorative glow blob top-left
        Box(
            modifier = Modifier
                .offset((-60).dp, (-60).dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(listOf(Indigo700.copy(alpha = 0.5f), Color.Transparent)),
                )
        )
        // Decorative glow blob bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(listOf(Indigo500.copy(alpha = 0.3f), Color.Transparent)),
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { -40 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(listOf(Indigo500, Indigo700))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⚡", fontSize = 36.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Hermes",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink50,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your personal AI agent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink200,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Connect to gateway",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink100,
                )

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

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("API Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = hermesTextFieldColors(),
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle",
                                tint = Ink200,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                )

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo500,
                        contentColor   = Color.White,
                    ),
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
            }
        }
    }
}

@Composable
fun hermesTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Indigo400,
    unfocusedBorderColor = Ink700,
    focusedLabelColor    = Indigo400,
    unfocusedLabelColor  = Ink200,
    cursorColor          = Indigo400,
    focusedTextColor     = Ink50,
    unfocusedTextColor   = Ink50,
    focusedContainerColor   = Ink800.copy(alpha = 0.5f),
    unfocusedContainerColor = Ink800.copy(alpha = 0.3f),
)
