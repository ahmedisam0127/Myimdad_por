package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.StockDao
import com.myimdad_por.data.local.entity.StockEntity
import com.myimdad_por.data.local.entity.toDomain as entityToDomain
import com.myimdad_por.data.mapper.toDomain as dtoToDomain
import com.myimdad_por.data.mapper.toDto
import com.myimdad_por.data.remote.datasource.InventoryRemoteDataSource
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.StockMovement
import com.myimdad_por.domain.repository.StockReservation
import com.myimdad_por.domain.repository.StockRepository
import com.myimdad_por.domain.repository.StockTransferResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StockRepositoryImpl @Inject constructor(
    private val stockDao: StockDao,
    private val remoteDataSource: InventoryRemoteDataSource,
    private val appDispatchers: AppDispatchers = DefaultAppDispatchers
) : StockRepository {

    private val movementLog = ConcurrentHashMap<String, MutableList<StockMovement>>()
    private val reservations = ConcurrentHashMap<String, StockReservation>()

    override fun observeAllStock(): Flow<List<StockItem>> {
        return stockDao.observeAll().map { entities ->
            entities.toDomainListSafely()
        }
    }

    override fun observeStockByBarcode(barcode: String): Flow<StockItem?> {
        val normalizedBarcode = barcode.normalizeBarcode()
        return stockDao.observeByNormalizedBarcode(normalizedBarcode).map { entities ->
            entities.firstOrNull()?.safeToDomain()
        }
    }

    override fun observeStockByLocation(location: String): Flow<List<StockItem>> {
        val normalizedLocation = location.normalizeLocation()
        return stockDao.observeByLocation(normalizedLocation).map { entities ->
            entities.toDomainListSafely()
        }
    }

    override fun observeLowStockItems(): Flow<List<StockItem>> {
        return stockDao.observeLowStock(DEFAULT_LOW_STOCK_THRESHOLD).map { entities ->
            entities.toDomainListSafely()
        }
    }

    override fun observeExpiredStockItems(): Flow<List<StockItem>> {
        return stockDao.observeAll().map { entities ->
            val today = LocalDate.now()
            entities.toDomainListSafely()
                .filter { item -> item.expiryDate?.isBefore(today) == true }
        }
    }

    override suspend fun getStockByBarcode(barcode: String): StockItem? = withContext(appDispatchers.io) {
        stockDao.observeAll().first()
            .firstOrNull { it.normalizedBarcode == barcode.normalizeBarcode() }
            ?.safeToDomain()
    }

    override suspend fun getStockItems(
        query: String?,
        location: String?,
        unitOfMeasure: UnitOfMeasure?
    ): List<StockItem> = withContext(appDispatchers.io) {
        stockDao.observeAll().first()
            .toDomainListSafely()
            .asSequence()
            .filter { item -> query.isNullOrBlank() || item.matchesQuery(query) }
            .filter { item ->
                location.isNullOrBlank() ||
                    item.location.normalizeLocation() == location.normalizeLocation()
            }
            .filter { item -> unitOfMeasure == null || item.unitOfMeasure == unitOfMeasure }
            .toList()
    }

    override suspend fun getStockHistory(
        barcode: String,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): List<StockMovement> = withContext(appDispatchers.io) {
        val key = barcode.normalizeBarcode()
        movementLog[key].orEmpty()
            .asSequence()
            .filter { movement -> from == null || movement.createdAtMillis >= from.toMillis() }
            .filter { movement -> to == null || movement.createdAtMillis <= to.toMillis() }
            .toList()
    }

    override suspend fun adjustStock(
        barcode: String,
        quantityDelta: Double,
        reason: String,
        location: String?
    ): Result<StockItem> = withContext(appDispatchers.io) {
        runCatching {
            require(quantityDelta.isFinite()) { "quantityDelta must be finite." }

            val normalizedBarcode = barcode.normalizeBarcode()
            val normalizedLocation = location?.normalizeLocation()

            val currentEntity = findBestStockEntity(normalizedBarcode, normalizedLocation)
                ?: throw NoSuchElementException("Stock item not found for barcode: $barcode")

            val currentItem = currentEntity.safeToDomain()
                ?: throw IllegalStateException("Failed to map stock entity: ${currentEntity.id}")

            val newQuantity = currentItem.quantity + quantityDelta
            require(newQuantity >= 0.0) { "Stock quantity cannot become negative." }

            val updatedItem = currentItem.copy(quantity = newQuantity)

            persistLocalStock(
                stockItem = updatedItem,
                existingEntity = currentEntity,
                syncState = "PENDING"
            )

            appendMovement(
                barcode = currentItem.productBarcode,
                quantityDelta = quantityDelta,
                sourceLocation = currentItem.location,
                targetLocation = currentItem.location,
                reason = reason,
                referenceId = null
            )

            syncStock(updatedItem, currentEntity, reason)
                .safeToDomain()
                ?: updatedItem
        }
    }

    override suspend fun transferStock(
        barcode: String,
        fromLocation: String,
        toLocation: String,
        quantity: Double,
        reason: String?
    ): Result<StockTransferResult> = withContext(appDispatchers.io) {
        runCatching {
            require(quantity.isFinite() && quantity > 0.0) { "quantity must be greater than zero." }

            val normalizedBarcode = barcode.normalizeBarcode()
            val sourceLocation = fromLocation.normalizeLocation()
            val targetLocation = toLocation.normalizeLocation()

            val sourceEntity = stockDao.getByBarcodeAndLocation(normalizedBarcode, sourceLocation)
                ?: throw NoSuchElementException("Source stock not found for barcode: $barcode at $fromLocation")

            val sourceItem = sourceEntity.safeToDomain()
                ?: throw IllegalStateException("Failed to map source stock: ${sourceEntity.id}")

            require(sourceItem.quantity >= quantity) {
                "Not enough stock to transfer. Available: ${sourceItem.quantity}, requested: $quantity"
            }

            val sourceUpdated = sourceItem.copy(quantity = sourceItem.quantity - quantity)
            persistLocalStock(
                stockItem = sourceUpdated,
                existingEntity = sourceEntity,
                syncState = "PENDING"
            )

            val targetEntity = stockDao.getByBarcodeAndLocation(normalizedBarcode, targetLocation)
            val targetUpdated = if (targetEntity == null) {
                sourceItem.copy(
                    quantity = quantity,
                    location = toLocation
                )
            } else {
                val targetItem = targetEntity.safeToDomain()
                    ?: throw IllegalStateException("Failed to map target stock: ${targetEntity.id}")
                targetItem.copy(
                    quantity = targetItem.quantity + quantity,
                    location = toLocation
                )
            }

            persistLocalStock(
                stockItem = targetUpdated,
                existingEntity = targetEntity,
                syncState = "PENDING"
            )

            val transferId = UUID.randomUUID().toString()
            appendMovement(
                barcode = sourceItem.productBarcode,
                quantityDelta = -quantity,
                sourceLocation = fromLocation,
                targetLocation = toLocation,
                reason = reason ?: "TRANSFER_OUT",
                referenceId = transferId
            )
            appendMovement(
                barcode = sourceItem.productBarcode,
                quantityDelta = quantity,
                sourceLocation = fromLocation,
                targetLocation = toLocation,
                reason = reason ?: "TRANSFER_IN",
                referenceId = transferId
            )

            StockTransferResult(
                transferId = transferId,
                barcode = sourceItem.productBarcode,
                fromLocation = fromLocation,
                toLocation = toLocation,
                quantity = quantity,
                completedAtMillis = System.currentTimeMillis(),
                metadata = mapOf("reason" to (reason ?: "TRANSFER"))
            )
        }
    }

    override suspend fun receiveStock(
        barcode: String,
        quantity: Double,
        location: String,
        sourceDocumentId: String?,
        reason: String?
    ): Result<StockItem> = withContext(appDispatchers.io) {
        runCatching {
            require(quantity.isFinite() && quantity > 0.0) { "quantity must be greater than zero." }

            val normalizedBarcode = barcode.normalizeBarcode()
            val normalizedLocation = location.normalizeLocation()

            val existing = stockDao.getByBarcodeAndLocation(normalizedBarcode, normalizedLocation)
            val template = existing?.safeToDomain()
                ?: findTemplateForBarcode(normalizedBarcode)
                ?: throw NoSuchElementException("No stock template found for barcode: $barcode")

            val updated = if (existing == null) {
                template.copy(quantity = quantity, location = location)
            } else {
                template.copy(quantity = template.quantity + quantity)
            }

            persistLocalStock(
                stockItem = updated,
                existingEntity = existing,
                syncState = "PENDING"
            )

            appendMovement(
                barcode = updated.productBarcode,
                quantityDelta = quantity,
                sourceLocation = sourceDocumentId,
                targetLocation = location,
                reason = reason ?: "RECEIVE",
                referenceId = sourceDocumentId
            )

            updated
        }
    }

    override suspend fun consumeStock(
        barcode: String,
        quantity: Double,
        location: String,
        reason: String?
    ): Result<StockItem> = withContext(appDispatchers.io) {
        runCatching {
            require(quantity.isFinite() && quantity > 0.0) { "quantity must be greater than zero." }

            val normalizedBarcode = barcode.normalizeBarcode()
            val normalizedLocation = location.normalizeLocation()

            val entity = stockDao.getByBarcodeAndLocation(normalizedBarcode, normalizedLocation)
                ?: throw NoSuchElementException("Stock item not found for barcode: $barcode at $location")

            val current = entity.safeToDomain()
                ?: throw IllegalStateException("Failed to map stock entity: ${entity.id}")

            require(current.quantity >= quantity) {
                "Not enough stock to consume. Available: ${current.quantity}, requested: $quantity"
            }

            val updated = current.copy(quantity = current.quantity - quantity)

            persistLocalStock(
                stockItem = updated,
                existingEntity = entity,
                syncState = "PENDING"
            )

            appendMovement(
                barcode = current.productBarcode,
                quantityDelta = -quantity,
                sourceLocation = location,
                targetLocation = null,
                reason = reason ?: "CONSUME",
                referenceId = null
            )

            updated
        }
    }

    override suspend fun reserveStock(
        barcode: String,
        quantity: Double,
        referenceId: String?
    ): Result<StockReservation> = withContext(appDispatchers.io) {
        runCatching {
            require(quantity.isFinite() && quantity > 0.0) { "quantity must be greater than zero." }

            val normalizedBarcode = barcode.normalizeBarcode()
            val entity = stockDao.observeAll().first()
                .firstOrNull { it.normalizedBarcode == normalizedBarcode && !it.isDeleted }
                ?: throw NoSuchElementException("Stock item not found for barcode: $barcode")

            val item = entity.safeToDomain()
                ?: throw IllegalStateException("Failed to map stock entity: ${entity.id}")

            require(item.quantity >= quantity) {
                "Not enough stock to reserve. Available: ${item.quantity}, requested: $quantity"
            }

            val updated = item.copy(quantity = item.quantity - quantity)
            persistLocalStock(
                stockItem = updated,
                existingEntity = entity,
                syncState = "PENDING"
            )

            val reservation = StockReservation(
                reservationId = UUID.randomUUID().toString(),
                barcode = item.productBarcode,
                quantity = quantity,
                referenceId = referenceId,
                createdAtMillis = System.currentTimeMillis(),
                expiresAtMillis = null,
                metadata = mapOf(
                    "location" to item.location,
                    "reason" to "RESERVE"
                )
            )

            reservations[reservation.reservationId] = reservation

            appendMovement(
                barcode = item.productBarcode,
                quantityDelta = -quantity,
                sourceLocation = item.location,
                targetLocation = null,
                reason = referenceId ?: "RESERVE",
                referenceId = reservation.reservationId
            )

            reservation
        }
    }

    override suspend fun releaseReservation(reservationId: String): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            val reservation = reservations.remove(reservationId) ?: return@runCatching Unit
            val location = reservation.metadata["location"].orEmpty()

            val entity = stockDao.getByBarcodeAndLocation(
                reservation.barcode.normalizeBarcode(),
                location.normalizeLocation()
            ) ?: throw NoSuchElementException("Stock item not found for reservation release: ${reservation.barcode}")

            val current = entity.safeToDomain()
                ?: throw IllegalStateException("Failed to map stock entity: ${entity.id}")

            val updated = current.copy(quantity = current.quantity + reservation.quantity)

            persistLocalStock(
                stockItem = updated,
                existingEntity = entity,
                syncState = "PENDING"
            )

            appendMovement(
                barcode = reservation.barcode,
                quantityDelta = reservation.quantity,
                sourceLocation = location,
                targetLocation = null,
                reason = "RELEASE_RESERVATION",
                referenceId = reservationId
            )

            Unit
        }
    }

    override suspend fun setReorderLevel(
        barcode: String,
        reorderLevel: Double
    ): Result<StockItem> = withContext(appDispatchers.io) {
        runCatching {
            throw UnsupportedOperationException(
                "setReorderLevel غير متاح لأن StockEntity الحالية لا تحتوي على حقل reorderLevel."
            )
        }
    }

    override suspend fun setStockExpiry(
        barcode: String,
        expiryDate: LocalDate
    ): Result<StockItem> = withContext(appDispatchers.io) {
        runCatching {
            val entity = stockDao.observeAll().first()
                .firstOrNull { it.normalizedBarcode == barcode.normalizeBarcode() }
                ?: throw NoSuchElementException("Stock item not found for barcode: $barcode")

            val current = entity.safeToDomain()
                ?: throw IllegalStateException("Failed to map stock entity: ${entity.id}")

            val updated = current.copy(expiryDate = expiryDate)
            persistLocalStock(
                stockItem = updated,
                existingEntity = entity,
                syncState = "PENDING"
            )

            updated
        }
    }

    override suspend fun clearExpiredStock(today: LocalDate): Result<Int> = withContext(appDispatchers.io) {
        runCatching {
            val thresholdMillis = today.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val expired = stockDao.observeAll().first()
                .filter { it.expiryDateMillis != null && it.expiryDateMillis < thresholdMillis }

            expired.forEach { entity ->
                stockDao.softDelete(
                    id = entity.id,
                    updatedAtMillis = System.currentTimeMillis(),
                    syncState = if (entity.serverId.isNullOrBlank()) "PENDING" else "SYNCED"
                )

                entity.serverId?.let { serverId ->
                    remoteDataSource.deleteStockEntry(serverId)
                }
            }

            expired.size
        }
    }

    override suspend fun deleteStockItem(barcode: String): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            val normalizedBarcode = barcode.normalizeBarcode()
            val entities = stockDao.observeAll().first()
                .filter { it.normalizedBarcode == normalizedBarcode }

            entities.forEach { entity ->
                stockDao.softDelete(
                    id = entity.id,
                    updatedAtMillis = System.currentTimeMillis(),
                    syncState = if (entity.serverId.isNullOrBlank()) "PENDING" else "SYNCED"
                )

                entity.serverId?.let { serverId ->
                    remoteDataSource.deleteStockEntry(serverId)
                }
            }

            Unit
        }
    }

    override suspend fun deleteStockItems(barcodes: List<String>): Result<Int> = withContext(appDispatchers.io) {
        runCatching {
            barcodes.distinct().count { deleteStockItem(it).isSuccess }
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            stockDao.observeAll().first().forEach { entity ->
                stockDao.delete(entity)
            }
            Unit
        }
    }

    override suspend fun countItems(): Long = withContext(appDispatchers.io) {
        stockDao.countActive().toLong()
    }

    override suspend fun countLowStockItems(): Long = withContext(appDispatchers.io) {
        stockDao.observeLowStock(DEFAULT_LOW_STOCK_THRESHOLD).first().size.toLong()
    }

    override suspend fun countExpiredStockItems(today: LocalDate): Long = withContext(appDispatchers.io) {
        val thresholdMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        stockDao.observeAll().first()
            .count { it.expiryDateMillis != null && it.expiryDateMillis < thresholdMillis }
            .toLong()
    }

    override suspend fun getAvailableQuantity(barcode: String): Double = withContext(appDispatchers.io) {
        val normalizedBarcode = barcode.normalizeBarcode()
        stockDao.observeAll().first()
            .filter { it.normalizedBarcode == normalizedBarcode }
            .sumOf { it.quantity }
    }

    override suspend fun getTotalQuantityByLocation(location: String): Double = withContext(appDispatchers.io) {
        val normalizedLocation = location.normalizeLocation()
        stockDao.observeAll().first()
            .filter { it.normalizedLocation == normalizedLocation }
            .sumOf { it.quantity }
    }

    private suspend fun syncStock(
        stockItem: StockItem,
        existingEntity: StockEntity?,
        movementReason: String? = null
    ): StockEntity {
        val localId = existingEntity?.id ?: stockItem.defaultStockId()
        val serverId = existingEntity?.serverId

        val request = stockItem.toDto(
            stockId = localId,
            serverId = serverId,
            movementType = "ADJUSTMENT",
            movementQuantity = stockItem.quantity,
            movementReason = movementReason,
            note = movementReason,
            syncState = "PENDING",
            isDeleted = false
        )

        val remoteResult = if (serverId.isNullOrBlank()) {
            remoteDataSource.createStockEntry(request)
        } else {
            remoteDataSource.updateStockEntry(serverId, request)
        }

        return when (remoteResult) {
            is NetworkResult.Success -> {
                val remoteDomain = remoteResult.data.dtoToDomain()
                val synced = StockEntity.fromDomain(
                    stockItem = remoteDomain,
                    serverId = remoteResult.data.serverId ?: remoteResult.data.stockId,
                    id = localId,
                    syncState = "SYNCED",
                    isDeleted = false,
                    syncedAtMillis = remoteResult.data.syncedAtMillis ?: System.currentTimeMillis(),
                    createdAtMillis = existingEntity?.createdAtMillis ?: System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )

                stockDao.update(synced)
                synced
            }

            is NetworkResult.Error -> existingEntity ?: persistFallback(stockItem, localId)
            NetworkResult.Loading -> existingEntity ?: persistFallback(stockItem, localId)
        }
    }

    private suspend fun persistLocalStock(
        stockItem: StockItem,
        existingEntity: StockEntity?,
        syncState: String
    ): StockEntity {
        val localId = existingEntity?.id ?: stockItem.defaultStockId()
        val entity = StockEntity.fromDomain(
            stockItem = stockItem,
            serverId = existingEntity?.serverId,
            id = localId,
            syncState = syncState,
            isDeleted = existingEntity?.isDeleted ?: false,
            syncedAtMillis = existingEntity?.syncedAtMillis,
            createdAtMillis = existingEntity?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )

        stockDao.insert(entity)
        return entity
    }

    private suspend fun persistFallback(
        stockItem: StockItem,
        localId: String
    ): StockEntity {
        val entity = StockEntity.fromDomain(
            stockItem = stockItem,
            serverId = null,
            id = localId,
            syncState = "PENDING",
            isDeleted = false,
            syncedAtMillis = null,
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )
        stockDao.insert(entity)
        return entity
    }

    private suspend fun findBestStockEntity(
        normalizedBarcode: String,
        normalizedLocation: String?
    ): StockEntity? {
        val all = stockDao.observeAll().first()
        return if (normalizedLocation.isNullOrBlank()) {
            all.firstOrNull { it.normalizedBarcode == normalizedBarcode }
        } else {
            all.firstOrNull {
                it.normalizedBarcode == normalizedBarcode &&
                    it.normalizedLocation == normalizedLocation
            } ?: all.firstOrNull { it.normalizedBarcode == normalizedBarcode }
        }
    }

    private suspend fun findTemplateForBarcode(normalizedBarcode: String): StockItem? {
        return stockDao.observeAll().first()
            .firstOrNull { it.normalizedBarcode == normalizedBarcode }
            ?.safeToDomain()
    }

    private fun appendMovement(
        barcode: String,
        quantityDelta: Double,
        sourceLocation: String?,
        targetLocation: String?,
        reason: String?,
        referenceId: String?
    ) {
        val key = barcode.normalizeBarcode()
        val list = movementLog.getOrPut(key) { mutableListOf() }

        synchronized(list) {
            list.add(
                StockMovement(
                    movementId = UUID.randomUUID().toString(),
                    barcode = barcode,
                    quantityDelta = quantityDelta,
                    sourceLocation = sourceLocation,
                    targetLocation = targetLocation,
                    reason = reason,
                    referenceId = referenceId,
                    createdAtMillis = System.currentTimeMillis(),
                    metadata = buildMap {
                        if (!reason.isNullOrBlank()) put("reason", reason)
                    }
                )
            )
        }
    }

    private fun StockEntity.safeToDomain(): StockItem? {
        return runCatching { entityToDomain() }.getOrNull()
    }

    private fun List<StockEntity>.toDomainListSafely(): List<StockItem> {
        return mapNotNull { it.safeToDomain() }
    }

    private fun String.normalizeBarcode(): String {
        return trim().lowercase(Locale.ROOT)
    }

    private fun String.normalizeLocation(): String {
        return trim().lowercase(Locale.ROOT)
    }

    private fun StockItem.matchesQuery(query: String): Boolean {
        val needle = query.trim().lowercase(Locale.ROOT)
        return productBarcode.contains(needle, ignoreCase = true) ||
            productName.contains(needle, ignoreCase = true) ||
            (displayName?.contains(needle, ignoreCase = true) == true) ||
            location.contains(needle, ignoreCase = true)
    }

    private fun StockItem.defaultStockId(): String {
        return "${productBarcode.normalizeBarcode()}@${location.normalizeLocation()}"
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    companion object {
        private const val DEFAULT_LOW_STOCK_THRESHOLD = 5.0
    }
}