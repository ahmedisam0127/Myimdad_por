package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.User
import java.math.BigDecimal
import javax.inject.Inject

enum class PrinterChannel {
    BLUETOOTH,
    USB,
    NETWORK,
    BUILT_IN,
    CLOUD
}

enum class PrinterDocumentType {
    RECEIPT,
    INVOICE,
    RETURN_NOTE,
    EXPENSE_VOUCHER,
    REPORT
}

data class PrinterAccessRequest(
    val user: User?,
    val documentType: PrinterDocumentType,
    val referenceId: String? = null,
    val branchId: String? = null,
    val targetDeviceId: String? = null,
    val channel: PrinterChannel = PrinterChannel.BLUETOOTH,
    val printerOnline: Boolean = true,
    val paperAvailable: Boolean = true,
    val retryCount: Int = 1,
    val maxRetryCount: Int = 3,
    val copies: Int = 1,
    val paymentMethod: PaymentMethod? = null,
    val amount: BigDecimal? = null,
    val content: String,
    val grantedTechnicalPermissions: Set<String> = emptySet(),
    val approvedPrinterDeviceId: String? = null,
    val approvedBranchId: String? = null,
    val context: PermissionContext = PermissionContext()
) {
    init {
        require(retryCount >= 0) { "retryCount cannot be negative" }
        require(maxRetryCount in 1..10) { "maxRetryCount must be between 1 and 10" }
        require(copies in 1..10) { "copies must be between 1 and 10" }
        require(content.isNotBlank()) { "content cannot be blank" }
        amount?.let {
            require(it >= BigDecimal.ZERO) { "amount cannot be negative" }
        }
    }
}

data class PreparedPrinterJob(
    val allowed: Boolean,
    val channel: PrinterChannel,
    val encodedPayload: String,
    val printCopies: Int,
    val hardwareDecision: HardwareAccessDecision,
    val reasons: List<String> = emptyList()
) {
    val isDenied: Boolean
        get() = !allowed
}

class RequestPrinterAccessUseCase @Inject constructor(
    private val authorizeHardwareAccessUseCase: AuthorizeHardwareAccessUseCase
) {

    operator fun invoke(request: PrinterAccessRequest): PreparedPrinterJob {
        val reasons = mutableListOf<String>()

        if (!request.printerOnline) {
            reasons += "الطابعة غير متصلة حاليًا"
        }

        if (!request.paperAvailable) {
            reasons += "الطابعة لا تحتوي على ورق"
        }

        val permissionDecision = authorizeHardwareAccessUseCase(
            user = request.user,
            request = HardwareAccessRequest(
                resource = HardwareResource.PRINTER,
                operation = HardwareOperation.PRINT,
                reason = when (request.documentType) {
                    PrinterDocumentType.INVOICE -> HardwareOperationReason.PRINT_INVOICE
                    PrinterDocumentType.RECEIPT -> HardwareOperationReason.PRINT_RECEIPT
                    else -> HardwareOperationReason.ADMIN_MAINTENANCE
                },
                referenceId = request.referenceId,
                branchId = request.branchId,
                targetDeviceId = request.targetDeviceId,
                requiredTechnicalPermissions = request.grantedTechnicalPermissions,
                metadata = mapOf(
                    "document_type" to request.documentType.name,
                    "channel" to request.channel.name,
                    "copies" to request.copies.toString(),
                    "amount" to (request.amount?.toPlainString() ?: "")
                )
            ),
            context = request.context,
            grantedTechnicalPermissions = request.grantedTechnicalPermissions,
            approvedPrinterDeviceId = request.approvedPrinterDeviceId,
            approvedBranchId = request.approvedBranchId
        )

        if (permissionDecision.isDenied) {
            reasons += permissionDecision.reasons
        }

        val payload = buildPrintablePayload(request)

        return PreparedPrinterJob(
            allowed = reasons.isEmpty(),
            channel = request.channel,
            encodedPayload = payload,
            printCopies = request.copies,
            hardwareDecision = permissionDecision,
            reasons = reasons
        )
    }

    private fun buildPrintablePayload(request: PrinterAccessRequest): String {
        return buildString {
            appendLine("DOCUMENT=${request.documentType.name}")
            appendLine("REFERENCE=${request.referenceId ?: ""}")
            appendLine("BRANCH=${request.branchId ?: ""}")
            appendLine("CHANNEL=${request.channel.name}")
            appendLine("COPIES=${request.copies}")
            appendLine("PAYMENT_METHOD=${request.paymentMethod?.displayName ?: ""}")
            appendLine("AMOUNT=${request.amount?.toPlainString() ?: ""}")
            appendLine("----")
            appendLine(request.content.trim())
        }
    }
}