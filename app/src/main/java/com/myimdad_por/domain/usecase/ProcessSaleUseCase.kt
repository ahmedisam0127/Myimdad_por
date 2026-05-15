package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.InvoiceLine
import com.myimdad_por.domain.model.InvoiceType
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.SaleStatus
import com.myimdad_por.domain.repository.SaleValidationResult
import com.myimdad_por.domain.repository.SalesRepository
import com.myimdad_por.domain.repository.StockRepository
import java.time.LocalDateTime
import javax.inject.Inject

data class ProcessSaleRequest(

    val sale: Sale,

    val defaultStockLocation: String? = null,

    val itemBarcodes: Map<String, String> = emptyMap(),

    val itemLocations: Map<String, String> = emptyMap(),

    val createInvoice: Boolean = true,

    val issueInvoiceNow: Boolean = true,

    val invoiceDueDate: LocalDateTime? = null,

    val allowReturnItems: Boolean = false,

    val invoicePartyId: String? = null,

    val invoicePartyName: String? = null,

    val invoicePartyTaxNumber: String? = null,

    val invoiceNotes: String? = null,

    val invoiceTermsAndConditions: String? = null
) {

    init {

        require(
            defaultStockLocation != null ||
                itemLocations.isNotEmpty()
        ) {
            "Either defaultStockLocation or itemLocations must be provided"
        }
    }
}

data class ProcessSaleResult(

    val sale: Sale,

    val validation: SaleValidationResult,

    val savedSale: Sale,

    val invoiceNumber: String? = null,

    val invoiceId: String? = null,

    val stockOperationsCount: Int = 0
)

class ProcessSaleUseCase @Inject constructor(

    private val salesRepository: SalesRepository,

    private val stockRepository: StockRepository,

    private val createInvoiceUseCase: CreateInvoiceUseCase
) {

    suspend operator fun invoke(
        request: ProcessSaleRequest
    ): Result<ProcessSaleResult> {

        return runCatching {

            /*
             * =========================================================
             * التحقق من صحة الفاتورة
             * =========================================================
             */
            val validation = salesRepository
                .validateSale(request.sale)
                .getOrThrow()

            require(validation.valid) {
                validation.message
                    ?: "فشل التحقق من عملية البيع"
            }

            /*
             * =========================================================
             * منع المرتجعات إذا كانت غير مسموحة
             * =========================================================
             */
            if (
                !request.allowReturnItems &&
                request.sale.hasReturns()
            ) {
                throw IllegalStateException(
                    "الفاتورة تحتوي على أصناف مرتجعة وغير مسموح بها"
                )
            }

            /*
             * =========================================================
             * حفظ الفاتورة
             * =========================================================
             */
            val savedSale = salesRepository
                .saveSale(request.sale)
                .getOrThrow()

            /*
             * =========================================================
             * التحقق من وجود أصناف
             * =========================================================
             */
            require(savedSale.items.isNotEmpty()) {
                "لا يمكن تنفيذ فاتورة بدون أصناف"
            }

            var stockOperationsCount = 0

            /*
             * =========================================================
             * معالجة كل صنف داخل الفاتورة
             * =========================================================
             */
            savedSale.items.forEach { item ->

                /*
                 * =====================================================
                 * تحديد الباركود الحقيقي
                 * =====================================================
                 */
                val barcode =
                    request.itemBarcodes[item.productId]
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: item.productId

                /*
                 * =====================================================
                 * تحديد موقع المخزن
                 * =====================================================
                 */
                val location =
                    request.itemLocations[item.productId]
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: request.defaultStockLocation
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException(
                            "لم يتم تحديد موقع المخزن للصنف: ${item.productName}"
                        )

                /*
                 * =====================================================
                 * التحقق من الكمية
                 * =====================================================
                 */
                require(item.quantity.toDouble() > 0.0) {
                    "الكمية غير صالحة للصنف: ${item.productName}"
                }

                /*
                 * =====================================================
                 * المرتجع => إضافة للمخزون
                 * =====================================================
                 */
                if (item.isReturn) {

                    stockRepository
                        .receiveStock(
                            barcode = barcode,

                            quantity = item.quantity.toDouble(),

                            location = location,

                            sourceDocumentId = savedSale.id,

                            reason =
                                "مرتجع فاتورة رقم ${savedSale.invoiceNumber}"
                        )
                        .getOrThrow()

                } else {

                    /*
                     * =================================================
                     * البيع => خصم من المخزون
                     * =================================================
                     */
                    stockRepository
                        .consumeStock(
                            barcode = barcode,

                            quantity = item.quantity.toDouble(),

                            location = location,

                            reason =
                                "بيع فاتورة رقم ${savedSale.invoiceNumber}"
                        )
                        .getOrThrow()
                }

                stockOperationsCount++
            }

            /*
             * =========================================================
             * تحديث حالة الفاتورة إلى مكتملة
             * =========================================================
             */
            val completedSale = if (
                savedSale.saleStatus != SaleStatus.COMPLETED
            ) {

                salesRepository
                    .markSaleCompleted(savedSale.id)
                    .getOrThrow()

                savedSale.copy(
                    saleStatus = SaleStatus.COMPLETED
                )

            } else {
                savedSale
            }

            /*
             * =========================================================
             * إنشاء الفاتورة المحاسبية
             * =========================================================
             */
            val invoice = if (request.createInvoice) {

                val invoiceLines =
                    completedSale.items.mapIndexed { index, item ->

                        item.toInvoiceLine(
                            barcode =
                                request.itemBarcodes[item.productId]
                                    ?: item.productId,

                            location =
                                request.itemLocations[item.productId]
                                    ?: request.defaultStockLocation,

                            lineIndex = index
                        )
                    }

                createInvoiceUseCase
                    .createFromSale(
                        saleId = completedSale.id,

                        invoiceType = InvoiceType.SALE,

                        lines = invoiceLines,

                        partyId =
                            request.invoicePartyId
                                ?: completedSale.customerId,

                        partyName = request.invoicePartyName,

                        partyTaxNumber =
                            request.invoicePartyTaxNumber,

                        issuedByEmployeeId =
                            completedSale.employeeId,

                        dueDate = request.invoiceDueDate,

                        notes =
                            request.invoiceNotes
                                ?: completedSale.note,

                        termsAndConditions =
                            request.invoiceTermsAndConditions
                    )
                    .getOrThrow()
                    .invoice

            } else {
                null
            }

            /*
             * =========================================================
             * النتيجة النهائية
             * =========================================================
             */
            ProcessSaleResult(

                sale = request.sale,

                validation = validation,

                savedSale = completedSale,

                invoiceNumber = invoice?.invoiceNumber,

                invoiceId = invoice?.id,

                stockOperationsCount = stockOperationsCount
            )
        }
    }

    /*
     * =============================================================
     * تحويل عنصر البيع إلى عنصر فاتورة
     * =============================================================
     */
    private fun SaleItem.toInvoiceLine(
        barcode: String,
        location: String?,
        lineIndex: Int
    ): InvoiceLine {

        return InvoiceLine(

            id = "${id}_$lineIndex",

            barcode = barcode,

            productName = productName,

            displayName = null,

            unitOfMeasure = unit,

            quantity = quantity,

            unitPrice = unitPrice,

            location = location,

            expiryDate = null,

            taxAmount = taxAmount,

            discountAmount = discountAmount,

            note = note
        )
    }
}