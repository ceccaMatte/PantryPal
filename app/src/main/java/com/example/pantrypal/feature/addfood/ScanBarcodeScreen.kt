package com.example.pantrypal.feature.addfood

import android.content.pm.PackageManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.ProductImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBarcodeScreen(
    state: ScanUiState,
    onEvent: (ScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check permission status on first composition
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        onEvent(ScanEvent.OnCameraPermissionResult(granted))
    }

    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    val barcodeAnalyzer = remember {
        BarcodeAnalyzer { barcode -> onEvent(ScanEvent.OnBarcodeDetected(barcode)) }
    }

    // Reset analyzer only on explicit user retry/dismiss (analyzerResetKey increments)
    LaunchedEffect(state.analyzerResetKey) {
        barcodeAnalyzer.reset()
    }

    // Bind camera when permission is granted
    DisposableEffect(lifecycleOwner, state.hasCameraPermission) {
        if (!state.hasCameraPermission) return@DisposableEffect onDispose { }

        var storedProvider: ProcessCameraProvider? = null
        val listener = Runnable {
            val provider = cameraProviderFuture.get()
            storedProvider = provider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer) }
            try {
                provider.unbindAll()
                cameraRef = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                cameraRef = null
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            storedProvider?.unbindAll()
            cameraRef = null
        }
    }

    // Torch control
    LaunchedEffect(state.torchEnabled, cameraRef) {
        cameraRef?.cameraControl?.enableTorch(state.torchEnabled)
    }

    // ProductRecognized bottom sheet
    state.recognizedProduct?.let { product ->
        ModalBottomSheet(
            onDismissRequest = { onEvent(ScanEvent.OnDismissRecognizedProduct) },
            containerColor = PantryColors.Card
        ) {
            ProductRecognizedSheet(product = product, onEvent = onEvent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101713))
    ) {
        // Background: real camera preview or dark placeholder
        if (state.hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ScannerBackdrop(Modifier.fillMaxSize())
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                IconButton(
                    onClick = { onEvent(ScanEvent.OnBackClick) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                }
            }
            Text("Scansione Codice", style = PantryTypography.headlineMedium, color = Color.White)
            Surface(
                shape = CircleShape,
                color = if (state.torchEnabled) Color(0xFFFFCC2F) else Color.Black.copy(alpha = 0.35f)
            ) {
                IconButton(
                    onClick = { onEvent(ScanEvent.OnTorchClick) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = "Torcia",
                        tint = if (state.torchEnabled) Color(0xFF121712) else Color.White
                    )
                }
            }
        }

        // Corner frame overlay (only when camera active)
        if (state.hasCameraPermission) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
            ) {
                val corner = 54.dp.toPx()
                val stroke = 4.dp.toPx()
                drawLine(Color.White, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(0f, 0f), Offset(0f, corner), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, 0f), Offset(size.width - corner, 0f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, 0f), Offset(size.width, corner), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(0f, size.height), Offset(corner, size.height), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(0f, size.height), Offset(0f, size.height - corner), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, size.height), Offset(size.width - corner, size.height), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, size.height), Offset(size.width, size.height - corner), stroke, StrokeCap.Round)
                drawLine(
                    Color.White.copy(alpha = 0.7f),
                    Offset(24.dp.toPx(), size.height / 2f),
                    Offset(size.width - 24.dp.toPx(), size.height / 2f),
                    stroke,
                    StrokeCap.Round
                )
            }
        }

        // Bottom panel
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = PantryColors.Card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PantrySpacing.lg)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 6.dp)
                        .background(PantryColors.Line, RoundedCornerShape(8.dp))
                )

                when {
                    !state.hasCameraPermission -> PermissionRequestContent(
                        isRequesting = state.isRequestingPermission,
                        onRequestPermission = { onEvent(ScanEvent.OnRequestCameraPermissionClick) }
                    )
                    state.isLookingUp || state.isProcessingBarcode -> ScanningActiveContent(
                        label = if (state.isLookingUp) "Ricerca prodotto…" else "Analisi barcode…"
                    )
                    else -> ScanReadyContent(
                        label = state.statusLabel,
                        showRetry = state.showRetryButton,
                        onRetry = { onEvent(ScanEvent.OnRetryScanClick) }
                    )
                }

                Button(
                    onClick = { onEvent(ScanEvent.OnManualClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PantryColors.Card,
                        contentColor = PantryColors.Green700
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        "Inserisci manualmente",
                        style = PantryTypography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(isRequesting: Boolean, onRequestPermission: () -> Unit) {
    if (isRequesting) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = PantryColors.Green700,
                strokeWidth = 3.dp
            )
            Text("Richiesta permesso…", style = PantryTypography.titleMedium, color = PantryColors.Muted)
        }
    } else {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = PantryColors.Green700,
            modifier = Modifier.size(40.dp)
        )
        Text(
            "Scansiona barcode",
            style = PantryTypography.headlineMedium
        )
        Text(
            "Consenti l'accesso alla fotocamera per leggere il codice a barre",
            color = PantryColors.Muted,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PantryColors.Green700,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Consenti fotocamera", style = PantryTypography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScanningActiveContent(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = PantryColors.Green700,
            strokeWidth = 3.dp
        )
        Text(label, style = PantryTypography.titleMedium, color = PantryColors.Muted)
    }
}

@Composable
private fun ScanReadyContent(
    label: String,
    showRetry: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PantryColors.Green700)
        Text(label, style = PantryTypography.titleMedium, color = PantryColors.Muted)
    }
    if (showRetry) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Riprova scansione", style = PantryTypography.titleMedium)
        }
    }
}

@Composable
private fun ProductRecognizedSheet(product: ProductRecognizedUi, onEvent: (ScanEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.lg)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 64.dp, height = 6.dp)
                .background(PantryColors.Line, RoundedCornerShape(8.dp))
        )
        Text("Prodotto riconosciuto", style = PantryTypography.headlineMedium)
        Text(product.subtitle, color = PantryColors.Muted)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = PantryColors.Background
        ) {
            Row(
                modifier = Modifier.padding(PantrySpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    modifier = Modifier.size(78.dp),
                    background = PantryColors.WarningBg.copy(alpha = 0.55f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(PantrySpacing.xs), modifier = Modifier.weight(1f)) {
                    Text(product.title, style = PantryTypography.titleLarge)
                    product.quantityLabel?.let { Text(it, color = PantryColors.Muted) }
                    if (product.suggestedCategoryLabels.isNotEmpty()) {
                        Text(
                            "Suggerimenti: ${product.suggestedCategoryLabels.joinToString(", ")}",
                            color = PantryColors.Green700,
                            style = PantryTypography.labelLarge
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onEvent(ScanEvent.OnUseRecognizedProductClick) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PantryColors.Green700,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Aggiungi o modifica", style = PantryTypography.titleMedium, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { onEvent(ScanEvent.OnDismissRecognizedProduct) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Non è questo", style = PantryTypography.titleMedium)
        }
        Spacer(Modifier.height(PantrySpacing.sm))
    }
}

@Composable
private fun ScannerBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF172019))
        val lineColor = Color.White.copy(alpha = 0.03f)
        val spacing = 36.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(
                lineColor,
                Offset(x, 0f),
                Offset(x + size.height, size.height),
                strokeWidth = 18.dp.toPx(),
                cap = StrokeCap.Square
            )
            x += spacing
        }
        drawCircle(
            color = Color.Black.copy(alpha = 0.16f),
            radius = size.minDimension * 0.8f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = size.minDimension * 0.55f)
        )
    }
}
