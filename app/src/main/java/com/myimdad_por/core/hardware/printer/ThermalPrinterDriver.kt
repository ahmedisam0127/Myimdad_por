package com.myimdad_por.core.hardware.printer

import com.myimdad_por.core.hardware.contracts.IPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset

/**
 * Thermal printer driver with a pluggable transport.
 *
 * This class renders a simple ESC/POS-like payload, but the actual transport
 * is injected so the same driver can work with Bluetooth, USB, or network.
 */
class ThermalPrinterDriver(
    private val transportHandler: PrinterTransportHandler,
    private val defaultCharset: Charset = Charsets.UTF_8
) : IPrinter {

    /**
     * واجهة للتعامل مع النقل الفيزيائي للبيانات.
     * تم إزالة كلمة 'fun' لأنها تحتوي على أكثر من دالة مجردة.
     */
    interface PrinterTransportHandler {
        suspend fun connect(connection: PrinterConnection)
        suspend fun disconnect(connection: PrinterConnection?)
        suspend fun send(connection: PrinterConnection, payload: ByteArray)
        suspend fun isPaperOut(connection: PrinterConnection): Boolean
    }

    private val _connectionStatus = MutableStateFlow<PrinterStatus>(PrinterStatus.Disconnected)
    override val connectionStatus: StateFlow<PrinterStatus> = _connectionStatus.asStateFlow()

    private var currentConnection: PrinterConnection? = null

    override suspend fun connect(address: String): Result<Unit> {
        return connect(
            PrinterConnection(
                address = address,
                // استخدام الـ Enum المعرف في الملف رقم 44
                transport = com.myimdad_por.core.hardware.printer.PrinterTransport.UNKNOWN
            )
        )
    }

    override suspend fun connect(connection: PrinterConnection): Result<Unit> {
        require(connection.address.isNotBlank()) { "connection address must not be blank." }

        return runCatching {
            _connectionStatus.value = PrinterStatus.Connecting
            transportHandler.connect(connection)
            currentConnection = connection
            _connectionStatus.value = PrinterStatus.Connected
        }.onFailure { error ->
            currentConnection = null
            _connectionStatus.value = PrinterStatus.Error(error.message ?: "Printer connection failed.")
        }
    }

    override suspend fun disconnect() {
        runCatching {
            transportHandler.disconnect(currentConnection)
        }
        currentConnection = null
        _connectionStatus.value = PrinterStatus.Disconnected
    }

    override suspend fun print(job: PrintJob): Result<Unit> {
        val connection = currentConnection
            ?: return Result.failure(IllegalStateException("Printer is not connected."))

        return runCatching {
            val payload = render(job)
            transportHandler.send(connection, payload)
        }.onFailure { error ->
            _connectionStatus.value = PrinterStatus.Error(error.message ?: "Printing failed.")
        }
    }

    override suspend fun isPaperOut(): Boolean {
        val connection = currentConnection ?: return true
        return runCatching {
            transportHandler.isPaperOut(connection)
        }.getOrDefault(true)
    }

    fun render(job: PrintJob): ByteArray {
        val builder = StringBuilder()

        job.title?.takeIf { it.isNotBlank() }?.let {
            builder.appendLine(it.trim())
            builder.appendLine()
        }

        repeat(job.copies) {
            job.normalizedLines().forEach { line ->
                builder.appendLine(applyAlignment(line, job.alignment))
            }

            repeat(job.feedPaperLines) {
                builder.appendLine()
            }
        }

        if (job.cutPaper) {
            // كود ESC/POS لقص الورق
            builder.append("\u001DVA0")
        }

        return builder.toString().toByteArray(defaultCharset)
    }

    private fun applyAlignment(text: String, alignment: PrintAlignment): String {
        return when (alignment) {
            PrintAlignment.LEFT -> text
            PrintAlignment.CENTER -> text.padStart((text.length + 10).coerceAtMost(40))
            PrintAlignment.RIGHT -> text.padStart((text.length + 20).coerceAtMost(48))
        }
    }
}
