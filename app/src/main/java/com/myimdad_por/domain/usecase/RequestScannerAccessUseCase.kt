package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.User
import javax.inject.Inject

enum class ScannerSource {
    BACK_CAMERA,
    FRONT_CAMERA,
    EXTERNAL_BLUETOOTH_SCANNER,
    EXTERNAL_USB_SCANNER
}

enum class BarcodeSymbology {
    QR_CODE,
    CODE_128,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    ITF,
    PDF_417,
    DATA_MATRIX,
    AZTEC
}

data class ScannerAccessRequest(
    val user: User?,
    val preferExternalScanner: Boolean = true,
    val preferFrontCamera: Boolean = false,
    val flashRequested: Boolean = true,
    val allowedSymbologies: Set<BarcodeSymbology> = setOf(
        BarcodeSymbology.QR_CODE,
        BarcodeSymbology.CODE_128,
        BarcodeSymbology.EAN_13,
        BarcodeSymbology.EAN_8,
        BarcodeSymbology.UPC_A
    ),
    val batteryLevelPercent: Int? = null,
    val isCharging: Boolean = false,
    val bluetoothScannerConnected: Boolean = false,
    val usbScannerConnected: Boolean = false,
    val referenceId: String? = null,
    val branchId: String? = null,
    val grantedTechnicalPermissions: Set<String> = emptySet(),
    val context: PermissionContext = PermissionContext()
) {
    init {
        batteryLevelPercent?.let {
            require(it in 0..100) { "batteryLevelPercent must be between 0 and 100" }
        }
    }
}

data class ScannerAccessPlan(
    val allowed: Boolean,
    val source: ScannerSource?,
    val flashEnabled: Boolean,
    val symbologies: Set<BarcodeSymbology>,
    val hardwareDecision: HardwareAccessDecision,
    val reasons: List<String> = emptyList()
) {
    val isDenied: Boolean
        get() = !allowed
}

class RequestScannerAccessUseCase @Inject constructor(
    private val authorizeHardwareAccessUseCase: AuthorizeHardwareAccessUseCase
) {

    operator fun invoke(request: ScannerAccessRequest): ScannerAccessPlan {
        val reasons = mutableListOf<String>()

        val source = resolveScannerSource(request, reasons)

        if (request.batteryLevelPercent != null &&
            request.batteryLevelPercent < LOW_BATTERY_THRESHOLD &&
            !request.isCharging &&
            source == ScannerSource.BACK_CAMERA
        ) {
            reasons += "البطارية منخفضة ولا يُنصح باستخدام الكاميرا في مسح طويل"
        }

        val hardwareRequest = HardwareAccessRequest(
            resource = HardwareResource.BARCODE_SCANNER,
            operation = HardwareOperation.SCAN,
            reason = HardwareOperationReason.BARCODE_SCAN,
            referenceId = request.referenceId,
            branchId = request.branchId,
            targetDeviceId = when (source) {
                ScannerSource.EXTERNAL_BLUETOOTH_SCANNER -> "bluetooth_scanner"
                ScannerSource.EXTERNAL_USB_SCANNER -> "usb_scanner"
                else -> null
            },
            requiredTechnicalPermissions = request.grantedTechnicalPermissions,
            metadata = mapOf(
                "scanner_source" to (source?.name ?: "unknown"),
                "flash_requested" to request.flashRequested.toString(),
                "prefer_external" to request.preferExternalScanner.toString()
            )
        )

        val authorization = authorizeHardwareAccessUseCase(
            user = request.user,
            request = hardwareRequest,
            context = request.context,
            grantedTechnicalPermissions = request.grantedTechnicalPermissions
        )

        if (authorization.isDenied) {
            reasons += authorization.reasons
        }

        return ScannerAccessPlan(
            allowed = reasons.isEmpty(),
            source = source,
            flashEnabled = request.flashRequested && source != ScannerSource.EXTERNAL_USB_SCANNER,
            symbologies = request.allowedSymbologies,
            hardwareDecision = authorization,
            reasons = reasons
        )
    }

    private fun resolveScannerSource(
        request: ScannerAccessRequest,
        reasons: MutableList<String>
    ): ScannerSource? {
        return when {
            request.preferExternalScanner && request.bluetoothScannerConnected ->
                ScannerSource.EXTERNAL_BLUETOOTH_SCANNER

            request.preferExternalScanner && request.usbScannerConnected ->
                ScannerSource.EXTERNAL_USB_SCANNER

            request.preferFrontCamera ->
                ScannerSource.FRONT_CAMERA

            else -> {
                if (!request.preferExternalScanner &&
                    !request.bluetoothScannerConnected &&
                    !request.usbScannerConnected
                ) {
                    reasons += "لم يتم العثور على ماسح خارجي، وسيتم الاعتماد على الكاميرا الخلفية"
                }
                ScannerSource.BACK_CAMERA
            }
        }
    }

    private companion object {
        private const val LOW_BATTERY_THRESHOLD = 15
    }
}