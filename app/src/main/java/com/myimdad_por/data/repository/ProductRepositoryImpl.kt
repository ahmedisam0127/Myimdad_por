package com.myimdad_por.data.repository
import javax.inject.Singleton

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.data.local.dao.ProductDao
import com.myimdad_por.data.local.entity.ProductEntity
import com.myimdad_por.data.local.entity.toDomain
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.ProductRepository
import com.myimdad_por.domain.repository.ProductStockSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import javax.inject.Inject
import java.math.RoundingMode
import java.util.UUID
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : ProductRepository {

    override fun observeAllProducts(): Flow<List<Product>> {
        return productDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeProductByBarcode(barcode: String): Flow<Product?> {
        val normalized = barcode.trim()
        if (normalized.isBlank()) return flowOf(null)

        return productDao.observeAll().map { list ->
            list.firstOrNull { it.barcode == normalized || it.normalizedBarcode == normalized }?.toDomain()
        }.flowOn(dispatchers.io)
    }

    override fun observeProductsByName(query: String): Flow<List<Product>> {
        val normalized = query.trim()
        if (normalized.isBlank()) return observeAllProducts()

        return productDao.search(normalized)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeLowStockProducts(threshold: BigDecimal?): Flow<List<ProductStockSnapshot>> {
        return productDao.observeActive().map { entities ->
            entities.map { it.toStockSnapshot() }
                .filter { snapshot ->
                    val limit = threshold ?: snapshot.reorderLevel
                    snapshot.quantityOnHand <= limit
                }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getProductByBarcode(barcode: String): Product? = withContext(dispatchers.io) {
        val normalized = barcode.trim()
        if (normalized.isBlank()) return@withContext null

        productDao.getByNormalizedBarcode(normalized)?.toDomain()
            ?: productDao.getByBarcode(normalized)?.toDomain()
    }

    override suspend fun getProductByName(name: String): Product? = withContext(dispatchers.io) {
        val normalized = name.trim()
        if (normalized.isBlank()) return@withContext null

        productDao.observeAll().first()
            .firstOrNull { it.name.equals(normalized, ignoreCase = true) }?.toDomain()
    }

    override suspend fun searchProducts(query: String): List<Product> = withContext(dispatchers.io) {
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext emptyList()

        productDao.search(normalized).first().map { it.toDomain() }
    }

    override suspend fun getProductsByIds(ids: List<String>): List<Product> = withContext(dispatchers.io) {
        ids.mapNotNull { id ->
            val normalized = id.trim()
            if (normalized.isBlank()) return@mapNotNull null
            
            productDao.getById(normalized)?.toDomain()
                ?: productDao.getByBarcode(normalized)?.toDomain()
        }
    }

    override suspend fun getProductsByUnit(unitOfMeasure: UnitOfMeasure): List<Product> = withContext(dispatchers.io) {
        productDao.observeByUnitOfMeasure(unitOfMeasure.name).first().map { it.toDomain() }
    }

    override suspend fun saveProduct(product: Product): Result<Product> = withContext(dispatchers.io) {
        runCatching {
            val normalizedBarcode = product.normalizedBarcode
            val existing = productDao.getByNormalizedBarcode(normalizedBarcode)
                ?: productDao.getByBarcode(product.barcode)

            val baseEntity = ProductEntity.fromDomain(
                product = product,
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )

            val entityToSave = baseEntity.copy(
                id = existing?.id ?: baseEntity.id,
                metadataJson = existing?.metadataJson ?: "{}"
            )

            productDao.insert(entityToSave)
            product
        }
    }

    override suspend fun saveProducts(products: List<Product>): Result<List<Product>> = withContext(dispatchers.io) {
        runCatching {
            if (products.isEmpty()) return@runCatching emptyList()

            products.map { product ->
                saveProduct(product).getOrElse { product }
            }
        }
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        return saveProduct(product)
    }

    override suspend fun updateProductPrice(barcode: String, price: BigDecimal): Result<Product> = withContext(dispatchers.io) {
        runCatching {
            val normalized = barcode.trim()
            val existing = productDao.getByNormalizedBarcode(normalized)
                ?: productDao.getByBarcode(normalized)
                ?: error("Product not found with barcode: $barcode")

            productDao.updatePrice(
                id = existing.id,
                price = price.moneyText(),
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )

            productDao.getById(existing.id)!!.toDomain()
        }
    }

    override suspend fun updateProductState(barcode: String, isActive: Boolean): Result<Product> = withContext(dispatchers.io) {
        runCatching {
            val normalized = barcode.trim()
            val existing = productDao.getByNormalizedBarcode(normalized)
                ?: productDao.getByBarcode(normalized)
                ?: error("Product not found with barcode: $barcode")

            productDao.updateActiveState(
                id = existing.id,
                isActive = isActive,
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )

            productDao.getById(existing.id)!!.toDomain()
        }
    }

    override suspend fun deleteProduct(barcode: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalized = barcode.trim()
            val existing = productDao.getByNormalizedBarcode(normalized)
                ?: productDao.getByBarcode(normalized)
                ?: return@runCatching Unit

            productDao.softDelete(
                id = existing.id,
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )
            Unit
        }
    }

    override suspend fun deleteProducts(barcodes: List<String>): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            var count = 0
            barcodes.mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct().forEach {
                if (deleteProduct(it).isSuccess) count++
            }
            count
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val allProducts = productDao.observeAll().first()
            allProducts.forEach { productDao.softDelete(it.id) }
            productDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countProducts(): Long = withContext(dispatchers.io) {
        productDao.countActive().toLong()
    }

    override suspend fun countActiveProducts(): Long = withContext(dispatchers.io) {
        productDao.observeActive().first().size.toLong()
    }

    override suspend fun countLowStockProducts(threshold: BigDecimal?): Long = withContext(dispatchers.io) {
        getLowStockProducts(threshold).size.toLong()
    }

    override suspend fun getStockSnapshot(barcode: String): ProductStockSnapshot? = withContext(dispatchers.io) {
        val normalized = barcode.trim()
        if (normalized.isBlank()) return@withContext null

        val entity = productDao.getByNormalizedBarcode(normalized)
            ?: productDao.getByBarcode(normalized)
            ?: return@withContext null

        entity.toStockSnapshot()
    }

    override suspend fun getLowStockProducts(threshold: BigDecimal?): List<ProductStockSnapshot> = withContext(dispatchers.io) {
        productDao.observeActive().first()
            .map { it.toStockSnapshot() }
            .filter { snapshot ->
                val limit = threshold ?: snapshot.reorderLevel
                snapshot.quantityOnHand <= limit
            }
    }

    override suspend fun syncProductStock(
        barcode: String,
        quantityDelta: BigDecimal,
        reason: String?
    ): Result<ProductStockSnapshot> = withContext(dispatchers.io) {
        runCatching {
            val normalized = barcode.trim()
            val existing = productDao.getByNormalizedBarcode(normalized)
                ?: productDao.getByBarcode(normalized)
                ?: error("Product not found with barcode: $barcode")

            val json = runCatching { JSONObject(existing.metadataJson) }.getOrDefault(JSONObject())
            val currentStock = json.optString("stockQuantity", "0").toBigDecimalOrZero()
            val newStock = (currentStock + quantityDelta).coerceAtLeast(BigDecimal.ZERO)

            json.put("stockQuantity", newStock.moneyText())
            reason?.takeIf { it.isNotBlank() }?.let {
                json.put("lastStockUpdateReason", it)
            }

            val updatedEntity = existing.copy(
                metadataJson = json.toString(),
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )

            productDao.update(updatedEntity)
            updatedEntity.toStockSnapshot()
        }
    }

    // --- Private Helpers ---

    private fun ProductEntity.toStockSnapshot(): ProductStockSnapshot {
        val json = runCatching { JSONObject(this.metadataJson) }.getOrDefault(JSONObject())
        val stockQty = json.optString("stockQuantity", "0").toBigDecimalOrZero()
        val reorder = json.optString("reorderLevel", "0").toBigDecimalOrZero()
        
        return ProductStockSnapshot(
            barcode = this.barcode,
            productName = this.name,
            displayName = this.displayName,
            unitOfMeasure = runCatching { UnitOfMeasure.valueOf(this.unitOfMeasure) }.getOrDefault(UnitOfMeasure.UNIT),
            quantityOnHand = stockQty,
            reorderLevel = reorder,
            lastUpdatedMillis = this.updatedAtMillis
        )
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(this) }
            .getOrDefault(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.moneyText(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal {
        return if (this < min) min else this
    }
}
