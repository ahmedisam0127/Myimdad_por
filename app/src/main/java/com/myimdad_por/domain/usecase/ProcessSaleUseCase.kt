package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.InvoiceLine
import com.myimdad_por.domain.model.InvoiceType
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.SaleStatus
import com.myimdad_por.domain.repository.SalesRepository
import com.myimdad_por.domain.repository.StockRepository
import java.math.BigDecimal
import javax.inject.Inject

data class ProcessSaleRequest(
    val sale: Sale,
    val defaultStockLocation: String? = null,
    val itemBarcodes: Map<String, String> = emptyMap(),
    val itemLocations: Map<String, String> = emptyMap(),
    val createInvoice: Boolean = true,
    val issueInvoiceNow: Boolean = true,
    val invoiceDueDate: java.time.LocalDateTime? = null,
    val allowReturnItems: Boolean = false,
    val invoicePartyId: String? = null,
    val invoicePartyName: String? = null,
    val invoicePartyTaxNumber: String? = null,
    val invoiceNotes: String? = null,
    val invoiceTermsAndConditions: String? = null
) {
    init {
        require(defaultStockLocation != null || itemLocations.isNotEmpty()) {
            "Either defaultStockLocation or itemLocations must be provided"
        }
    }
}

data class ProcessSaleResult(
    val sale: Sale,
    val validation: com.myimdad_por.domain.repository.SaleValidationResult,
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

    suspend operator fun invoke(request: ProcessSaleRequest): Result<ProcessSaleResult> {
        return runCatching {
            val validation = salesRepository.validateSale(request.sale).getOrThrow()
            require(validation.valid) {
                validation.message ?: "Sale validation failed"
            }

            if (!request.allowReturnItems && request.sale.hasReturns()) {
                throw IllegalStateException("Sale contains return items and allowReturnItems is false")
            }

            val savedSale = salesRepository.saveSale(request.sale).getOrThrow()

            var stockOps = 0

            for (item in savedSale.items) {
                val barcode = request.itemBarcodes[item.productId] ?: item.productId
                val location = request.itemLocations[item.productId] ?: request.defaultStockLocation
                    ?: throw IllegalStateException("Missing stock location for productId=${item.productId}")

                if (item.isReturn) {
                    stockRepository.receiveStock(
                        barcode = barcode,
                        quantity = item.quantity.toDouble(),
                        location = location,
                        sourceDocumentId = savedSale.id,
                        reason = "Return on sale ${savedSale.invoiceNumber}"
                    ).getOrThrow()
                } else {
                    stockRepository.consumeStock(
                        barcode = barcode,
                        quantity = item.quantity.toDouble(),
                        location = location,
                        reason = "Sale ${savedSale.invoiceNumber}"
                    ).getOrThrow()
                }
                stockOps++
            }

            if (savedSale.saleStatus != SaleStatus.COMPLETED) {
                salesRepository.markSaleCompleted(savedSale.id).getOrThrow()
            }

            val invoice = if (request.createInvoice) {
                val lines = savedSale.items.mapIndexed { index, item ->
                    item.toInvoiceLine(
                        barcode = request.itemBarcodes[item.productId] ?: item.productId,
                        location = request.itemLocations[item.productId] ?: request.defaultStockLocation,
                        lineIndex = index
                    )
                }

                createInvoiceUseCase.createFromSale(
                    saleId = savedSale.id,
                    invoiceType = InvoiceType.SALE,
                    lines = lines,
                    partyId = request.invoicePartyId ?: savedSale.customerId,
                    partyName = request.invoicePartyName,
                    partyTaxNumber = request.invoicePartyTaxNumber,
                    issuedByEmployeeId = savedSale.employeeId,
                    dueDate = request.invoiceDueDate,
                    notes = request.invoiceNotes ?: savedSale.note,
                    termsAndConditions = request.invoiceTermsAndConditions
                ).getOrThrow().invoice
            } else {
                null
            }

            ProcessSaleResult(
                sale = request.sale,
                validation = validation,
                savedSale = savedSale,
                invoiceNumber = invoice?.invoiceNumber,
                invoiceId = invoice?.id,
                stockOperationsCount = stockOps
            )
        }
    }

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