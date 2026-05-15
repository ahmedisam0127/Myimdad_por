package com.myimdad_por.domain.usecase.Product
import javax.inject.Inject
data class ProductUseCases @Inject constructor(
    val validateProductUseCase: ValidateProductUseCase,
    val createProductUseCase: CreateProductUseCase,
    val getProductUseCase: GetProductUseCase,
    val getProductsUseCase: GetProductsUseCase,
    val searchProductsUseCase: SearchProductsUseCase,
    val filterProductsUseCase: FilterProductsUseCase,
    val updateProductPriceUseCase: UpdateProductPriceUseCase,
    val updateProductStockUseCase: UpdateProductStockUseCase,
    val increaseProductStockUseCase: IncreaseProductStockUseCase,
    val decreaseProductStockUseCase: DecreaseProductStockUseCase,
    val convertProductUnitUseCase: ConvertProductUnitUseCase,
    val syncProductsUseCase: SyncProductsUseCase,
    val deleteProductUseCase: DeleteProductUseCase,
    val getTopSellingProductsUseCase: GetTopSellingProductsUseCase
)