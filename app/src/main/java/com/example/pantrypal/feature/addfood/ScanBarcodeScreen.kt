package com.example.pantrypal.feature.addfood

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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography

@Composable
fun ScanBarcodeScreen(
    state: ScanUiState,
    onEvent: (ScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101713))
    ) {
        ScannerBackdrop(Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.25f)) {
                IconButton(onClick = { onEvent(ScanEvent.OnBackClick) }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                }
            }
            Text("Scansione Codice", style = PantryTypography.headlineMedium, color = Color.White)
            Surface(shape = CircleShape, color = Color(0xFFFFCC2F)) {
                IconButton(onClick = { onEvent(ScanEvent.OnTorchClick) }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Torcia", tint = Color(0xFF121712))
                }
            }
        }

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
                Color.White.copy(alpha = 0.9f),
                Offset(24.dp.toPx(), size.height / 2f),
                Offset(size.width - 24.dp.toPx(), size.height / 2f),
                stroke,
                StrokeCap.Round
            )
        }

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)) {
                    if (state.isReading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PantryColors.Green700, strokeWidth = 3.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PantryColors.Green700)
                    }
                    Text(state.statusLabel, style = PantryTypography.titleMedium, color = PantryColors.Muted)
                }
                Button(
                    onClick = { onEvent(ScanEvent.OnManualClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = PantryColors.Card, contentColor = PantryColors.Green700),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Inserisci manualmente", style = PantryTypography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
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
