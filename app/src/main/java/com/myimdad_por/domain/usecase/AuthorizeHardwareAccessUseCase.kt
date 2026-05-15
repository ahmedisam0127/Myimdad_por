package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.User
import javax.inject.Inject

enum class HardwareResource {
    CASH_DRAWER,
    PRINTER,
    BARCODE_SCANNER,
    CAMERA,
    USB_DEVICE,
    BLUETOOTH_DEVICE
}

enum class HardwareOperation {
    OPEN,
    CONNECT,
    DISCONNECT,
    PRINT,
    SCAN,
    CAPTURE,
    SYNC
}

enum class HardwareOperationReason {
    SALE_COMPLETED,
    EXPENSE_PAID,
    REFUND_APPROVED,
    CASH_COUNTING,
    PRINT_RECEIPT,
    PRINT_INVOICE,
    BARCODE_SCAN,
    ADMIN_MAINTENANCE,
    OTHER
}

data class HardwareAccessRequest(
    val resource: HardwareResource,
    val operation: HardwareOperation,
    val reason: HardwareOperationReason = HardwareOperationReason.OTHER,
    val referenceId: String? = null,
    val branchId: String? = null,
    val targetDeviceId: String? = null,
    val requiredTechnicalPermissions: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
)

data class HardwareAccessDecision(
    val allowed: Boolean,
    val resource: HardwareResource,
    val reasons: List<String> = emptyList()
) {
    val isDenied: Boolean
        get() = !allowed
}

/**
 * Hilt-friendly use case.
 * يعتمد على CheckPermissionUseCase بدل تكرار المنطق.
 */
class AuthorizeHardwareAccessUseCase @Inject constructor(
    private val checkPermissionUseCase: CheckPermissionUseCase
) {

    operator fun invoke(
        user: User?,
        request: HardwareAccessRequest,
        context: PermissionContext = PermissionContext(),
        grantedTechnicalPermissions: Set<String> = emptySet(),
        approvedPrinterDeviceId: String? = null,
        approvedBranchId: String? = null
    ): HardwareAccessDecision {
        val reasons = mutableListOf<String>()

        if (user == null) {
            reasons += "المستخدم غير موجود"
            return denied(request.resource, reasons)
        }

        val requiredPermission = requiredDomainPermission(request)
        if (requiredPermission != null) {
            val permissionDecision = checkPermissionUseCase(
                user = user,
                permission = requiredPermission,
                context = context
            )
            if (permissionDecision.isDenied) {
                reasons += permissionDecision.reasons
            }
        }

        when (request.resource) {
            HardwareResource.CASH_DRAWER -> {
                if (request.referenceId.isNullOrBlank()) {
                    reasons += "فتح درج النقد يتطلب مرجعًا ماليًا مسجلًا"
                }

                if (request.reason !in allowedCashDrawerReasons) {
                    reasons += "سبب فتح درج النقد غير مسموح"
                }

                if (!context.registerOpen) {
                    reasons += "لا يمكن فتح درج النقد والصندوق مغلق"
                }
            }

            HardwareResource.PRINTER -> {
                if (request.targetDeviceId.isNullOrBlank()) {
                    reasons += "الطابعة الهدف غير محددة"
                }

                if (approvedPrinterDeviceId != null &&
                    request.targetDeviceId != approvedPrinterDeviceId
                ) {
                    reasons += "الطابعة المحددة غير معتمدة لهذا الفرع"
                }

                if (approvedBranchId != null &&
                    request.branchId != null &&
                    request.branchId != approvedBranchId
                ) {
                    reasons += "الفرع المحدد لا يملك هذه الطابعة المعتمدة"
                }
            }

            HardwareResource.BARCODE_SCANNER,
            HardwareResource.CAMERA -> {
                val missingTechnical = request.requiredTechnicalPermissions.filterNot {
                    it in grantedTechnicalPermissions
                }
                if (missingTechnical.isNotEmpty()) {
                    reasons += missingTechnical.map { "الصلاحية التقنية غير متوفرة: $it" }
                }
            }

            HardwareResource.USB_DEVICE -> Unit

            HardwareResource.BLUETOOTH_DEVICE -> {
                if (request.targetDeviceId.isNullOrBlank()) {
                    reasons += "الجهاز البلوتوثي الهدف غير محدد"
                }
            }
        }

        return if (reasons.isEmpty()) {
            HardwareAccessDecision(
                allowed = true,
                resource = request.resource
            )
        } else {
            denied(request.resource, reasons)
        }
    }

    private fun requiredDomainPermission(request: HardwareAccessRequest): Permission? {
        return when (request.resource) {
            HardwareResource.CASH_DRAWER -> Permission.MANAGE_PAYMENTS
            HardwareResource.PRINTER -> Permission.MANAGE_PRINTER
            HardwareResource.BARCODE_SCANNER -> Permission.MANAGE_SCANNER
            HardwareResource.CAMERA -> Permission.ACCESS_HARDWARE
            HardwareResource.USB_DEVICE -> Permission.ACCESS_HARDWARE
            HardwareResource.BLUETOOTH_DEVICE -> Permission.ACCESS_HARDWARE
        }
    }

    private fun denied(
        resource: HardwareResource,
        reasons: List<String>
    ): HardwareAccessDecision {
        return HardwareAccessDecision(
            allowed = false,
            resource = resource,
            reasons = reasons
        )
    }

    private val allowedCashDrawerReasons = setOf(
        HardwareOperationReason.SALE_COMPLETED,
        HardwareOperationReason.EXPENSE_PAID,
        HardwareOperationReason.REFUND_APPROVED,
        HardwareOperationReason.CASH_COUNTING
    )
}