package com.myimdad_por.core.hardware.printer

/**
 * Transport type for a printer connection.
 */
enum class PrinterTransport {
    BLUETOOTH,
    USB,
    TCP_IP,
    SERIAL,
    UNKNOWN
}

/**
 * Connection model for printers.
 */
data class PrinterConnection(
    val address: String,
    val transport: PrinterTransport = PrinterTransport.UNKNOWN,
    val port: Int? = null,
    val displayName: String? = null,
    val secure: Boolean = false
) {
    init {
        require(address.isNotBlank()) { "address must not be blank." }
        port?.let {
            require(it in 1..65535) { "port must be between 1 and 65535." }
        }
    }
}

/**
 * Printer state exposed to the UI and services.
 */
sealed class PrinterStatus {
    data object Disconnected : PrinterStatus()
    data object Connecting : PrinterStatus()
    data object Connected : PrinterStatus()
    data class Error(val message: String) : PrinterStatus()
}