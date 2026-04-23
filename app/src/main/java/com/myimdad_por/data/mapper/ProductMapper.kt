package com.myimdad_por.data.mapper

import com.myimdad_por.data.local.entity.ProductEntity
import com.myimdad_por.data.remote.dto.ProductDto
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * محول بيانات المنتجات الاحترافي
 * يربط بين (الشبكة، قاعدة البيانات، والمنطق البرمجي) دون كسر البنية الحالية.
 */
object ProductMapper {

    // --- من قاعدة البيانات (Entity) إلى المنطق (Domain) ---
    
    fun toDomain(entity: ProductEntity): Product {
        return Product(
            barcode = entity.barcode,
            name = entity.name,
            price = entity.price.toBigDecimalOrZero(),
            unitOfMeasure = runCatching { UnitOfMeasure.valueOf(entity.unitOfMeasure) }
                .getOrDefault(UnitOfMeasure.UNIT),
            displayName = entity.displayName,
            description = entity.description,
            isActive = entity.isActive
        )
    }

    // --- من المنطق (Domain) إلى قاعدة البيانات (Entity) ---
    
    fun toEntity(domain: Product, existingId: String? = null): ProductEntity {
        val id = existingId ?: UUID.randomUUID().toString()
        return ProductEntity(
            id = id,
            barcode = domain.barcode,
            normalizedBarcode = domain.normalizedBarcode,
            name = domain.name,
            displayName = domain.displayName,
            description = domain.description,
            unitOfMeasure = domain.unitOfMeasure.name,
            price = domain.price.toMoneyString(),
            isActive = domain.isActive,
            searchTokens = buildSearchTokens(domain),
            metadataJson = "{}",
            syncState = "PENDING",
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    // --- من الشبكة (Dto) إلى قاعدة البيانات (Entity) ---
    
    fun fromDtoToEntity(dto: ProductDto): ProductEntity {
        val barcode = dto.barcode ?: dto.sku ?: "NO_BARCODE_${System.currentTimeMillis()}"
        return ProductEntity(
            id = dto.id ?: UUID.randomUUID().toString(),
            serverId = dto.serverId,
            barcode = barcode,
            normalizedBarcode = barcode.trim(),
            name = dto.name,
            displayName = dto.name, // افتراضياً
            description = dto.description,
            unitOfMeasure = dto.unitName?.uppercase() ?: UnitOfMeasure.UNIT.name,
            price = (dto.sellingPrice ?: "0.00").toBigDecimalOrZero().toMoneyString(),
            isActive = dto.isActive,
            searchTokens = "${dto.name} $barcode ${dto.categoryName ?: ""}".lowercase().trim(),
            syncState = "SYNCED",
            syncedAtMillis = System.currentTimeMillis()
        )
    }

    // --- تحويل القوائم (List Mappers) ---

    fun toDomainList(entities: List<ProductEntity>): List<Product> = entities.map { toDomain(it) }
    
    fun toEntityList(domains: List<Product>): List<ProductEntity> = domains.map { toEntity(it) }

    // --- أدوات مساعدة خاصة (Private Helpers) ---

    private fun buildSearchTokens(product: Product): String {
        return listOfNotNull(
            product.barcode.lowercase(),
            product.name.lowercase(),
            product.displayName?.lowercase(),
            product.unitOfMeasure.name.lowercase()
        ).distinct().joinToString(" ")
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(this) }
            .getOrDefault(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.toMoneyString(): String {
        return this.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}
