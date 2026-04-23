package com.myimdad_por.core.hardware.scanner

import com.myimdad_por.core.hardware.contracts.IBarcodeScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Generic scanner driver that can be fed by any concrete scanner backend.
 *
 * It does not depend on a specific camera or scanner SDK.
 * A concrete adapter can call [publishScan] whenever a code is detected.
 */
class BarcodeScannerDriver(
    private val duplicateWindowMillis: Long = 750L,
    private val onFlashChanged: (Boolean) -> Unit = {}
) : IBarcodeScanner {

    enum class ScannerState {
        IDLE,
        SCANNING,
        STOPPED,
        ERROR
    }

    private val _scanResults = MutableSharedFlow<ScanResult>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val scanResults: Flow<ScanResult> = _scanResults.asSharedFlow()

    private val _state = MutableStateFlow(ScannerState.IDLE)
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private var lastEmittedValue: String? = null
    private var lastEmittedAtMillis: Long = 0L
    private var flashEnabled: Boolean = false

    override fun startScanning() {
        _state.value = ScannerState.SCANNING
    }

    override fun stopScanning() {
        _state.value = ScannerState.STOPPED
    }

    override fun isFlashSupported(): Boolean = true

    override fun toggleFlash(isEnabled: Boolean) {
        flashEnabled = isEnabled
        onFlashChanged(isEnabled)
    }

    fun isFlashEnabled(): Boolean = flashEnabled

    override fun publishScan(result: ScanResult) {
        if (_state.value != ScannerState.SCANNING) return
        if (!result.isValid) return
        if (isDuplicate(result)) return

        lastEmittedValue = result.normalizedValue
        lastEmittedAtMillis = result.scannedAtMillis
        _scanResults.tryEmit(result)
    }

    fun submitRawScan(
        rawValue: String,
        format: BarcodeFormat = BarcodeFormat.UNKNOWN,
        source: ScanSource = ScanSource.EXTERNAL_SCANNER,
        confidence: Int? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        publishScan(
            ScanResult(
                rawValue = rawValue,
                format = format,
                source = source,
                confidence = confidence,
                metadata = metadata
            )
        )
    }

    fun reportError(message: String) {
        require(message.isNotBlank()) { "message must not be blank." }
        _state.value = ScannerState.ERROR
    }

    private fun isDuplicate(result: ScanResult): Boolean {
        val value = result.normalizedValue
        val previousValue = lastEmittedValue ?: return false
        val timeDelta = abs(result.scannedAtMillis - lastEmittedAtMillis)

        return previousValue == value && timeDelta <= duplicateWindowMillis
    }
}