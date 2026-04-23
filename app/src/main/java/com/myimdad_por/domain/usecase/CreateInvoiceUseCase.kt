package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Invoice
import com.myimdad_por.domain.model.InvoiceLine
import com.myimdad_por.domain.model.InvoiceStatus
import com.myimdad_por.domain.model.InvoiceType
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.repository.InvoiceRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.inject.Inject

data class CreateInvoiceRequest(
    val invoiceType: InvoiceType,
    val lines: List<InvoiceLine>,
    val partyId: String? = null,
    val partyName: String? = null,
    val partyTaxNumber: String? = null,
    val issuedByEmployeeId: String? = null,
    val invoiceNumber: String? = null,
    val issueDate: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val taxAmount: BigDecimal? = null,
    val discountAmount: BigDecimal? = null,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val termsAndConditions: String? = null,
    val issueNow: Boolean = true
) {
    init {
        require(lines.isNotEmpty()) { "lines cannot be empty" }
        require(paidAmount >= BigDecimal.ZERO) { "paidAmount cannot be negative" }
        taxAmount?.let { require(it >= BigDecimal.ZERO) { "taxAmount cannot be negative" } }
        discountAmount?.let { require(it >= BigDecimal.ZERO) { "discountAmount cannot be negative" } }
        dueDate?.let { require(!it.isBefore(issueDate)) { "dueDate cannot be before issueDate" } }
    }
}

data class CreateInvoiceResult(
    val invoice: Invoice,
    val invoiceNumber: String,
    val paymentStatus: PaymentStatus,
    val subtotalAmount: BigDecimal,
    val totalAmount: BigDecimal
)

class CreateInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {

    suspend operator fun invoke(request: CreateInvoiceRequest): Result<CreateInvoiceResult> {
        return runCatching {
            val invoiceNumber = request.invoiceNumber
                ?: invoiceRepository.getNextInvoiceNumber()

            val subtotalAmount = request.lines.sumMoney { it.subtotalAmount }
            val taxAmount = request.taxAmount ?: request.lines.sumMoney { it.taxAmount }
            val discountAmount = request.discountAmount ?: request.lines.sumMoney { it.discountAmount }

            val totalAmount = subtotalAmount
                .add(taxAmount)
                .subtract(discountAmount)
                .money()

            val invoice = Invoice(
                invoiceNumber = invoiceNumber,
                invoiceType = request.invoiceType,
                status = when {
                    request.issueNow && request.paidAmount >= totalAmount && totalAmount > BigDecimal.ZERO ->
                        InvoiceStatus.PAID
                    request.issueNow ->
                        InvoiceStatus.ISSUED
                    else ->
                        InvoiceStatus.DRAFT
                },
                issueDate = request.issueDate,
                dueDate = request.dueDate,
                partyId = request.partyId,
                partyName = request.partyName,
                partyTaxNumber = request.partyTaxNumber,
                issuedByEmployeeId = request.issuedByEmployeeId,
                lines = request.lines,
                taxAmount = taxAmount.money(),
                discountAmount = discountAmount.money(),
                paidAmount = request.paidAmount.money(),
                notes = request.notes,
                termsAndConditions = request.termsAndConditions
            )

            invoiceRepository.saveInvoice(invoice).getOrThrow()

            CreateInvoiceResult(
                invoice = invoice,
                invoiceNumber = invoiceNumber,
                paymentStatus = invoice.paymentStatus,
                subtotalAmount = subtotalAmount.money(),
                totalAmount = totalAmount
            )
        }
    }

    suspend fun createFromSale(
        saleId: String,
        invoiceType: InvoiceType,
        lines: List<InvoiceLine>,
        partyId: String? = null,
        partyName: String? = null,
        partyTaxNumber: String? = null,
        issuedByEmployeeId: String? = null,
        dueDate: LocalDateTime? = null,
        notes: String? = null,
        termsAndConditions: String? = null
    ): Result<CreateInvoiceResult> {
        require(saleId.isNotBlank()) { "saleId cannot be blank" }
        return invoke(
            CreateInvoiceRequest(
                invoiceType = invoiceType,
                lines = lines,
                partyId = partyId,
                partyName = partyName,
                partyTaxNumber = partyTaxNumber,
                issuedByEmployeeId = issuedByEmployeeId,
                dueDate = dueDate,
                notes = notes,
                termsAndConditions = termsAndConditions,
                issueNow = true
            )
        )
    }

    private inline fun List<InvoiceLine>.sumMoney(selector: (InvoiceLine) -> BigDecimal): BigDecimal {
        return fold(BigDecimal.ZERO) { acc, line -> acc.add(selector(line)) }.money()
    }

    private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)
}