package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.CustomerSnapshot
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SaleInvoiceStatus
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

object SalesPreviewData {

    private val now: LocalDateTime =
        DateTimeUtils.now()

    val previewCustomers: List<Customer> = listOf(
        Customer(
            id = "customer_001",
            code = "CUS-001",
            fullName = "شركة النيل التجارية",
            tradeName = "النيل",
            phoneNumber = "0912345678",
            email = "info@nile.sd",
            address = "الخرطوم - السوق العربي",
            city = "الخرطوم",
            country = "السودان",
            taxNumber = "TX-10001",
            creditLimit = BigDecimal("500000"),
            outstandingBalance = BigDecimal("120000")
        ),
        Customer(
            id = "customer_002",
            code = "CUS-002",
            fullName = "مؤسسة البركة",
            tradeName = "البركة",
            phoneNumber = "0923456789",
            address = "بحري - المنطقة الصناعية",
            city = "بحري",
            country = "السودان",
            creditLimit = BigDecimal("250000"),
            outstandingBalance = BigDecimal.ZERO
        ),
        Customer(
            id = "customer_003",
            code = "CUS-003",
            fullName = "أحمد محمد عبدالله",
            phoneNumber = "0999999999",
            address = "أم درمان",
            city = "أم درمان",
            country = "السودان",
            creditLimit = BigDecimal("100000"),
            outstandingBalance = BigDecimal("15000")
        )
    )

    val previewProducts: List<Product> = listOf(
        Product(
            barcode = "628100000001",
            name = "سكر كنانة",
            displayName = "سكر كنانة 50 كجم",
            description = "سكر أبيض فاخر",
            price = BigDecimal("28500"),
            unitOfMeasure = UnitOfMeasure.SHAWL,
            largeUnit = UnitOfMeasure.SHAWL,
            smallUnit = UnitOfMeasure.KILOGRAM,
            unitFactor = BigDecimal("50")
        ),
        Product(
            barcode = "628100000002",
            name = "زيت طبخ",
            displayName = "زيت طبخ 36 رطل",
            description = "زيت نباتي نقي",
            price = BigDecimal("72000"),
            unitOfMeasure = UnitOfMeasure.CARTON,
            largeUnit = UnitOfMeasure.CARTON,
            smallUnit = UnitOfMeasure.BOTTLE,
            unitFactor = BigDecimal("12")
        ),
        Product(
            barcode = "628100000003",
            name = "مكرونة",
            displayName = "مكرونة ممتازة",
            description = "عبوة 400 جرام",
            price = BigDecimal("2500"),
            unitOfMeasure = UnitOfMeasure.BOX,
            largeUnit = UnitOfMeasure.CARTON,
            smallUnit = UnitOfMeasure.BOX,
            unitFactor = BigDecimal("24")
        ),
        Product(
            barcode = "628100000004",
            name = "أرز",
            displayName = "أرز بسمتي",
            description = "جودة ممتازة",
            price = BigDecimal("34000"),
            unitOfMeasure = UnitOfMeasure.SHAWL,
            largeUnit = UnitOfMeasure.SHAWL,
            smallUnit = UnitOfMeasure.KILOGRAM,
            unitFactor = BigDecimal("25")
        )
    )

    val previewProductUiModels: List<ProductUiModel> =
        previewProducts.map(ProductUiModel::fromDomain)

    val previewSaleItems: List<SaleItem> = listOf(
        SaleItem(
            id = UUID.randomUUID().toString(),
            saleId = "sale_preview_001",
            productId = "628100000001",
            productName = "سكر كنانة 50 كجم",
            unit = UnitOfMeasure.SHAWL,
            quantity = BigDecimal("2"),
            unitPrice = BigDecimal("28500"),
            taxAmount = BigDecimal("1500"),
            discountAmount = BigDecimal("1000")
        ),
        SaleItem(
            id = UUID.randomUUID().toString(),
            saleId = "sale_preview_001",
            productId = "628100000002",
            productName = "زيت طبخ 36 رطل",
            unit = UnitOfMeasure.CARTON,
            quantity = BigDecimal("1"),
            unitPrice = BigDecimal("72000"),
            taxAmount = BigDecimal("3000"),
            discountAmount = BigDecimal("2500")
        ),
        SaleItem(
            id = UUID.randomUUID().toString(),
            saleId = "sale_preview_001",
            productId = "628100000003",
            productName = "مكرونة ممتازة",
            unit = UnitOfMeasure.BOX,
            quantity = BigDecimal("5"),
            unitPrice = BigDecimal("2500"),
            taxAmount = BigDecimal.ZERO,
            discountAmount = BigDecimal("500")
        )
    )

    val previewCartItems: List<CartUiModel> =
        previewSaleItems.map { saleItem ->
            CartUiModel.fromSaleItem(
                saleItem = saleItem,
                product = previewProductUiModels.firstOrNull {
                    it.barcode == saleItem.productId
                }
            )
        }

    val previewInvoice: SaleInvoice = SaleInvoice(
        id = "invoice_preview_001",
        invoiceNumber = "INV-20260511-0001",
        saleId = "sale_preview_001",
        status = SaleInvoiceStatus.PARTIALLY_PAID,
        issueDate = now.minusHours(2),
        dueDate = now.plusDays(7),
        taxReference = "TX-INV-2026-001",
        customerSnapshot = CustomerSnapshot(
            name = previewCustomers.first().displayName,
            address = previewCustomers.first().address,
            taxNumber = previewCustomers.first().taxNumber,
            phone = previewCustomers.first().phoneNumber
        ),
        employeeId = "EMP-001",
        items = previewSaleItems,
        paidAmount = BigDecimal("50000"),
        notes = "يرجى التسليم قبل الساعة 5 مساءً",
        termsAndConditions = SalesConstants.Invoice.TERMS_AND_CONDITIONS,
        qrPayload = "SALE:INV-20260511-0001"
    )

    val previewBillUiModel: BillUiModel =
        BillUiModel.fromInvoice(
            invoice = previewInvoice,
            customer = previewCustomers.first(),
            paymentMethod = PaymentMethod.CASH,
            products = previewProductUiModels
        )

    val emptyBillUiModel: BillUiModel =
        BillUiModel.empty()

    val emptyCartUiModel: CartUiModel =
        CartUiModel.empty()

    val previewPaymentMethods: List<PaymentMethod> =
        PaymentMethod.values()

    val previewSearchQueries: List<String> = listOf(
        "سكر",
        "زيت",
        "628100000001",
        "مكرونة",
        "أرز"
    )

    val previewNotes: List<String> = listOf(
        "عميل دائم",
        "توصيل سريع",
        "دفع جزئي",
        "يرجى مراجعة الكميات"
    )

    fun randomProduct(): ProductUiModel {
        return previewProductUiModels.random()
    }

    fun randomCustomer(): Customer {
        return previewCustomers.random()
    }

    fun randomCartItem(): CartUiModel {
        return previewCartItems.random()
    }

    fun buildPreviewInvoice(
        invoiceNumber: String = "INV-PREVIEW-001",
        paymentMethod: PaymentMethod = PaymentMethod.CASH
    ): BillUiModel {
        return BillUiModel.fromInvoice(
            invoice = previewInvoice.copy(
                invoiceNumber = invoiceNumber
            ),
            customer = previewCustomers.first(),
            paymentMethod = paymentMethod,
            products = previewProductUiModels
        )
    }
}