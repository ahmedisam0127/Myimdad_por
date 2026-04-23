package com.myimdad_por.data.remote.dto

data class ProductDto(
    val id: String? = null,
    val serverId: String? = null,

    val name: String,
    val barcode: String? = null,
    val sku: String? = null,
    val description: String? = null,

    val categoryId: String? = null,
    val categoryName: String? = null,
    val unitId: String? = null,
    val unitName: String? = null,

    val costPrice: String? = null,
    val sellingPrice: String? = null,
    val wholesalePrice: String? = null,

    val taxRate: String? = null,
    val discountRate: String? = null,

    val stockQuantity: String? = null,
    val reservedQuantity: String? = null,
    val availableQuantity: String? = null,
    val lowStockThreshold: String? = null,

    val isActive: Boolean = true,
    val isFeatured: Boolean = false,

    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,

    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,

    val syncState: String? = null,
    val links: ProductLinksDto? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(name.isNotBlank()) { "name cannot be blank." }
    }
}

data class ProductLinksDto(
    val self: String? = null,
    val collection: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val image: String? = null,
    val thumbnail: String? = null,
    val stock: String? = null,
    val pricing: String? = null
)