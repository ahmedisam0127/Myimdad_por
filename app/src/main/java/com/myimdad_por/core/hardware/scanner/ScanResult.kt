package com.myimdad_por.core.hardware.scanner

/**
 * Barcode formats supported by the domain layer.
 */
enum class BarcodeFormat {
    UNKNOWN,
    QR_CODE,
    EAN_8,
    EAN_13,
    UPC_A,
    UPC_E,
    CODE_39,
    CODE_93,
    CODE_128,
    ITF,
    PDF_417,
    AZTEC,
    DATA_MATRIX,
    CODABAR
}

/**
 * Where the scan came from.
 */
enum class ScanSource {
    UNKNOWN,
    CAMERA,
    EXTERNAL_SCANNER,
    MANUAL_ENTRY,
    IMPORTED
}

/**
 * Immutable scan payload.
 */
data class ScanResult(
    val rawValue: String,
    val format: BarcodeFormat = BarcodeFormat.UNKNOWN,
    val source: ScanSource = ScanSource.UNKNOWN,
    val scannedAtMillis: Long = System.currentTimeMillis(),
    val confidence: Int? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(rawValue.isNotBlank()) { "rawValue must not be blank." }
        confidence?.let {
            require(it in 0..100) { "confidence must be between 0 and 100." }
        }
    }

    val normalizedValue: String
        get() = rawValue.trim()

    val isValid: Boolean
        get() = normalizedValue.isNotEmpty()
}