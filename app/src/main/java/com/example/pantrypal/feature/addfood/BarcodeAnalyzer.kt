package com.example.pantrypal.feature.addfood

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                // Only retail product barcodes — no QR, CODE_128, CODE_39
                // to avoid picking up lot numbers, promo codes, internal codes
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
    )

    private val hasEmittedBarcode = AtomicBoolean(false)

    // Consecutive-frame confirmation state (single-threaded: main executor)
    private var lastCandidate: String? = null
    private var candidateCount = 0

    fun reset() {
        hasEmittedBarcode.set(false)
        lastCandidate = null
        candidateCount = 0
    }

    private fun isValidProductBarcode(value: String): Boolean {
        val v = value.trim()
        return v.all { it.isDigit() } && v.length in setOf(8, 12, 13)
    }

    // If multiple barcodes in one frame, prefer the one with the largest bounding box
    // (centre of frame, most prominently displayed)
    private fun pickBestCandidate(barcodes: List<Barcode>): String? =
        barcodes
            .filter { isValidProductBarcode(it.rawValue ?: "") }
            .maxByOrNull { bc ->
                bc.boundingBox?.let { r -> r.width().toLong() * r.height().toLong() } ?: 0L
            }
            ?.rawValue
            ?.trim()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (hasEmittedBarcode.get()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val candidate = pickBestCandidate(barcodes)
                if (candidate == null) {
                    lastCandidate = null
                    candidateCount = 0
                    return@addOnSuccessListener
                }
                Log.d(TAG, "Barcode raw detected: $candidate")
                if (candidate == lastCandidate) {
                    candidateCount++
                } else {
                    lastCandidate = candidate
                    candidateCount = 1
                }
                // Accept only after 2 consistent consecutive frames to reduce false positives
                if (candidateCount >= 2 && hasEmittedBarcode.compareAndSet(false, true)) {
                    Log.d(TAG, "Barcode candidate accepted: $candidate (seen $candidateCount frames)")
                    onBarcodeDetected(candidate)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    companion object {
        private const val TAG = "PantryPalScan"
    }
}
