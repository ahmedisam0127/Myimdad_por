package com.myimdad_por.core.hardware.contracts

import com.myimdad_por.core.hardware.printer.PrintJob
import com.myimdad_por.core.hardware.printer.PrinterConnection
import com.myimdad_por.core.hardware.printer.PrinterStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for thermal printers or any printable output device.
 */
interface IPrinter {

    /**
     * Current printer connection status.
     */
    val connectionStatus: StateFlow<PrinterStatus>

    /**
     * Connect using a simple address string.
     * For Bluetooth this can be MAC address.
     * For network printers this can be an IP address.
     */
    suspend fun connect(address: String): Result<Unit>

    /**
     * Connect using a richer connection model.
     */
    suspend fun connect(connection: PrinterConnection): Result<Unit>

    /**
     * Disconnect from printer.
     */
    suspend fun disconnect()

    /**
     * Print a job.
     */
    suspend fun print(job: PrintJob): Result<Unit>

    /**
     * Returns true when printer has no paper.
     */
    suspend fun isPaperOut(): Boolean
}