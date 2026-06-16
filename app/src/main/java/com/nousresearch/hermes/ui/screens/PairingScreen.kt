package com.nousresearch.hermes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ConnectionViewModel
import com.nousresearch.hermes.viewmodel.PairingViewModel
import java.util.concurrent.Executors

@Composable
fun PairingScreen(
    onPaired: () -> Unit,
    onNavigateBack: () -> Unit,
    pairingVm: PairingViewModel    = hiltViewModel(),
    connectionVm: ConnectionViewModel = hiltViewModel(),
) {
    val state by pairingVm.state.collectAsState()
    val context = LocalContext.current

    var gatewayUrl   by remember { mutableStateOf("") }
    var manualCode   by remember { mutableStateOf("") }
    var showScanner  by remember { mutableStateOf(false) }
    var hasCamera    by remember { mutableStateOf(false) }
    var qrScanned    by remember { mutableStateOf(false) }

    // Camera permission
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
        if (granted) showScanner = true
    }

    // On successful redeem — save config and navigate
    LaunchedEffect(state.success, state.token) {
        if (state.success && state.token != null) {
            connectionVm.connect(
                state.gatewayUrl ?: gatewayUrl,
                state.token!!,
                state.profile,
            )
            onPaired()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0D0B2B), Color(0xFF0F0F1A), Color(0xFF060612)))
        )
    ) {
        // Glow blobs
        Box(modifier = Modifier.offset((-60).dp, (-40).dp).size(220.dp).background(Brush.radialGradient(listOf(Indigo700.copy(alpha = 0.4f), Color.Transparent))))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Back
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink200)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Header
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.QrCodeScanner, null, Modifier.size(34.dp), tint = Color.White) }

            Spacer(Modifier.height(16.dp))
            Text("Pair with Gateway", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink50)
            Spacer(Modifier.height(6.dp))
            Text(
                "Scan the QR code shown by\nhermes dispatch pair",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink200,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // QR scanner box OR placeholder
            AnimatedVisibility(visible = showScanner && hasCamera, enter = fadeIn(), exit = fadeOut()) {
                QrScannerView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, Indigo400.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                    onScanned = { raw ->
                        if (!qrScanned) {
                            qrScanned = true
                            parseHermesQr(raw)?.let { (url, code) ->
                                gatewayUrl = url
                                pairingVm.redeemCode(url, code)
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(visible = !showScanner || !hasCamera) {
                // Camera launch button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(2.dp, Ink700, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(52.dp), tint = Ink600)
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    hasCamera = true
                                    showScanner = true
                                } else {
                                    permLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Color.White),
                        ) {
                            Text("Open Camera", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(Modifier.weight(1f), color = Ink700)
                Text("  or enter code manually  ", style = MaterialTheme.typography.labelSmall, color = Ink600)
                HorizontalDivider(Modifier.weight(1f), color = Ink700)
            }

            Spacer(Modifier.height(16.dp))

            // Manual entry
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = gatewayUrl,
                    onValueChange = { gatewayUrl = it },
                    label = { Text("Gateway URL") },
                    placeholder = { Text("https://hermes.yourdomain.com", color = Ink600) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = hermesTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it.uppercase().take(9) },
                    label = { Text("Pairing code") },
                    placeholder = { Text("XXXX-XXXX", color = Ink600) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = hermesTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (gatewayUrl.isNotBlank() && manualCode.isNotBlank())
                            pairingVm.redeemCode(gatewayUrl, manualCode)
                    }),
                )

                AnimatedVisibility(visible = state.error != null) {
                    state.error?.let { err ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(10.dp),
                        ) { Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                Button(
                    onClick = { pairingVm.redeemCode(gatewayUrl, manualCode) },
                    enabled = gatewayUrl.isNotBlank() && manualCode.isNotBlank() && !state.isRedeeming,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Color.White),
                ) {
                    if (state.isRedeeming) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Pairing…")
                    } else {
                        Text("Pair", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // How to generate the code hint
            Text(
                "On your gateway server run:\n hermes dispatch pair",
                style = MaterialTheme.typography.bodySmall,
                color = Ink600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── QR scanner ────────────────────────────────────────────────────────────────

@Composable
private fun QrScannerView(modifier: Modifier, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                            ia.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val img = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(img)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                                ?.rawValue?.let(onScanned)
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}

// ── QR payload parser ─────────────────────────────────────────────────────────

private fun parseHermesQr(raw: String): Pair<String, String>? {
    return try {
        val uri = android.net.Uri.parse(raw)
        // hermes://pair?url=https://...&code=XXXX-XXXX
        if (uri.scheme == "hermes" && uri.host == "pair") {
            val url  = uri.getQueryParameter("url")  ?: return null
            val code = uri.getQueryParameter("code") ?: return null
            url to code
        } else null
    } catch (_: Exception) { null }
}
