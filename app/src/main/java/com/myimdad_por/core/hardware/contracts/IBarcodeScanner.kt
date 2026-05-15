package com.myimdad_por.core.hardware.contracts

import com.myimdad_por.core.hardware.scanner.ScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Contract for any barcode scanner implementation.
 *
 * A scanner can be backed by:
 * - device camera
 * - external laser scanner
 * - USB scanner
 * - Bluetooth scanner
 */
interface IBarcodeScanner {

    /**
     * Stream of scan results.
     */
    val scanResults: Flow<ScanResult>

    /**
     * Starts scanning.
     */
    fun startScanning()

    /**
     * Stops scanning.
     */
    fun stopScanning()

    /**
     * Returns true when flash/torch can be controlled.
     */
    fun isFlashSupported(): Boolean

    /**
     * Enables or disables flash/torch.
     */
    fun toggleFlash(isEnabled: Boolean)

    /**
     * Publishes a scan result from the underlying hardware source.
     */
    fun publishScan(result: ScanResult)
}