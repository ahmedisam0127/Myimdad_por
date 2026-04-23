package com.myimdad_por.core.hardware.printer

import com.myimdad_por.core.hardware.contracts.IPrinter
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level printer service.
 *
 * Keeps app code away from transport details and low-level driver concerns.
 */
class PrinterService(
    private val printer: IPrinter
) {

    val connectionStatus: StateFlow<PrinterStatus> = printer.connectionStatus

    suspend fun connect(address: String): Result<Unit> {
        return printer.connect(address)
    }

    suspend fun connect(connection: PrinterConnection): Result<Unit> {
        return printer.connect(connection)
    }

    suspend fun disconnect() {
        printer.disconnect()
    }

    suspend fun print(job: PrintJob): Result<Unit> {
        return printer.print(job)
    }

    suspend fun isPaperOut(): Boolean {
        return printer.isPaperOut()
    }

    suspend fun printReceipt(
        title: String?,
        lines: List<String>,
        copies: Int = 1,
        cutPaper: Boolean = true,
        alignment: PrintAlignment = PrintAlignment.LEFT,
        metadata: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return print(
            PrintJob(
                title = title,
                lines = lines,
                copies = copies,
                cutPaper = cutPaper,
                alignment = alignment,
                metadata = metadata
            )
        )
    }
}