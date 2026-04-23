package com.myimdad_por.core.hardware.scanner

import com.myimdad_por.core.hardware.contracts.IBarcodeScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Application-facing scanner service.
 *
 * Keeps scanner handling centralized and easy to replace.
 */
class ScannerService(
    private val scanner: IBarcodeScanner
) {

    val scanResults: Flow<ScanResult> = scanner.scanResults

    fun startScanning() {
        scanner.startScanning()
    }

    fun stopScanning() {
        scanner.stopScanning()
    }

    fun toggleFlash(isEnabled: Boolean) {
        if (scanner.isFlashSupported()) {
            scanner.toggleFlash(isEnabled)
        }
    }

    fun submitScan(
        rawValue: String,
        format: BarcodeFormat = BarcodeFormat.UNKNOWN,
        source: ScanSource = ScanSource.EXTERNAL_SCANNER,
        confidence: Int? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        scanner.publishScan(
            ScanResult(
                rawValue = rawValue,
                format = format,
                source = source,
                confidence = confidence,
                metadata = metadata
            )
        )
    }

    suspend fun scanOnce(timeoutMillis: Long = 10_000L): Result<ScanResult> {
        require(timeoutMillis > 0) { "timeoutMillis must be greater than 0." }

        return runCatching {
            val result = withTimeoutOrNull(timeoutMillis) {
                scanResults.first { it.isValid }
            } ?: throw java.util.concurrent.TimeoutException("No scan result received within timeout.")
            result
        }
    }
}