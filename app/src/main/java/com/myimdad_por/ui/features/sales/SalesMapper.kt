package com.myimdad_por.ui.features.sales

import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel

object SalesMapper {

    fun toProductUiModel(product: Product): ProductUiModel {
        return ProductUiModel.fromDomain(product)
    }

    fun toProductUiModels(products: Iterable<Product>): List<ProductUiModel> {
        return products.map(::toProductUiModel)
    }

    fun toCartUiModel(
        saleItem: SaleItem,
        product: ProductUiModel? = null
    ): CartUiModel {
        return CartUiModel.fromSaleItem(
            saleItem = saleItem,
            product = product
        )
    }

    fun toCartUiModels(
        saleItems: Iterable<SaleItem>,
        productsByBarcode: Map<String, ProductUiModel> = emptyMap()
    ): List<CartUiModel> {
        return saleItems.map { item ->
            toCartUiModel(
                saleItem = item,
                product = productsByBarcode[item.productId]
                    ?: productsByBarcode[item.productId.trim()]
            )
        }
    }

    fun toBillUiModel(
        invoice: SaleInvoice,
        customer: Customer? = null,
        paymentMethod: PaymentMethod? = null,
        products: List<ProductUiModel> = emptyList()
    ): BillUiModel {
        return BillUiModel.fromInvoice(
            invoice = invoice,
            customer = customer,
            paymentMethod = paymentMethod,
            products = products
        )
    }

    fun toBillsUiModels(
        invoices: Iterable<SaleInvoice>,
        customer: Customer? = null,
        paymentMethod: PaymentMethod? = null,
        products: List<ProductUiModel> = emptyList()
    ): List<BillUiModel> {
        return invoices.map { invoice ->
            toBillUiModel(
                invoice = invoice,
                customer = customer,
                paymentMethod = paymentMethod,
                products = products
            )
        }
    }

    fun productMapByBarcode(products: Iterable<ProductUiModel>): Map<String, ProductUiModel> {
        return products.associateBy { it.barcode }
    }

    fun productMapByBarcodeFromDomain(products: Iterable<Product>): Map<String, ProductUiModel> {
        return toProductUiModels(products).associateBy { it.barcode }
    }

    fun findProduct(
        products: Iterable<ProductUiModel>,
        barcode: String
    ): ProductUiModel? {
        val normalized = barcode.trim()
        if (normalized.isEmpty()) return null
        return products.firstOrNull { it.barcode == normalized }
    }

    fun findProductFromDomain(
        products: Iterable<Product>,
        barcode: String
    ): ProductUiModel? {
        val normalized = barcode.trim()
        if (normalized.isEmpty()) return null
        return products
            .firstOrNull { it.normalizedBarcode == normalized }
            ?.let(::toProductUiModel)
    }
}

fun Product.toUiModel(): ProductUiModel = SalesMapper.toProductUiModel(this)

fun Iterable<Product>.toUiModels(): List<ProductUiModel> =
    SalesMapper.toProductUiModels(this)

fun SaleItem.toUiModel(product: ProductUiModel? = null): CartUiModel =
    SalesMapper.toCartUiModel(this, product)

fun Iterable<SaleItem>.toUiModels(
    productsByBarcode: Map<String, ProductUiModel> = emptyMap()
): List<CartUiModel> = SalesMapper.toCartUiModels(this, productsByBarcode)

fun SaleInvoice.toUiModel(
    customer: Customer? = null,
    paymentMethod: PaymentMethod? = null,
    products: List<ProductUiModel> = emptyList()
): BillUiModel = SalesMapper.toBillUiModel(
    invoice = this,
    customer = customer,
    paymentMethod = paymentMethod,
    products = products
)

fun Iterable<SaleInvoice>.toUiModels(
    customer: Customer? = null,
    paymentMethod: PaymentMethod? = null,
    products: List<ProductUiModel> = emptyList()
): List<BillUiModel> = SalesMapper.toBillsUiModels(
    invoices = this,
    customer = customer,
    paymentMethod = paymentMethod,
    products = products
)