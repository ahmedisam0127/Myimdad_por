package com.myimdad_por.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.InvoiceDao
import com.myimdad_por.data.local.entity.InvoiceEntity
import com.myimdad_por.data.local.entity.toDomain as invoiceEntityToDomain
import com.myimdad_por.data.remote.datasource.SalesRemoteDataSource
import com.myimdad_por.data.remote.dto.CustomerSnapshotDto
import com.myimdad_por.data.remote.dto.SaleInvoiceDto
import com.myimdad_por.data.remote.dto.SaleItemDto
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.SaleStatus
import com.myimdad_por.domain.repository.SaleHold
import com.myimdad_por.domain.repository.SaleProductRanking
import com.myimdad_por.domain.repository.SaleValidationResult
import com.myimdad_por.domain.repository.SalesRepository
import com.myimdad_por.domain.repository.SortDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import java.util.UUID

class SalesRepositoryImpl @Inject constructor (
    private val invoiceDao: InvoiceDao,
    private val remoteDataSource: SalesRemoteDataSource,
    private val appDispatchers: AppDispatchers = DefaultAppDispatchers,
    private val gson: Gson = Gson()
) : SalesRepository {

    override fun observeAllSales(): Flow<List<Sale>> {
        return invoiceDao.observeAll().map { entities -> entities.toSalesListSafely() }
    }

    override fun observeSalesByCustomer(customerId: String): Flow<List<Sale>> {
        return invoiceDao.observeByPartyId(customerId).map { entities -> entities.toSalesListSafely() }
    }

    override fun observeSalesByEmployee(employeeId: String): Flow<List<Sale>> {
        return invoiceDao.observeByEmployeeId(employeeId).map { entities -> entities.toSalesListSafely() }
    }

    override fun observeSalesByStatus(status: SaleStatus): Flow<List<Sale>> {
        return invoiceDao.observeAll().map { entities ->
            entities.toSalesListSafely().filter { it.saleStatus == status }
        }
    }

    override fun observePendingSales(): Flow<List<Sale>> {
        return invoiceDao.observePendingSync().map { entities ->
            entities.toSalesListSafely().filter { it.isPendingLike() }
        }
    }

    override suspend fun getSaleById(id: String): Sale? = withContext(appDispatchers.io) {
        invoiceDao.getById(id)?.safeToSale()
    }

    override suspend fun getSaleByInvoiceNumber(invoiceNumber: String): Sale? = withContext(appDispatchers.io) {
        invoiceDao.getByInvoiceNumber(invoiceNumber)?.safeToSale()
    }

    override suspend fun getSales(
        from: LocalDateTime?,
        to: LocalDateTime?,
        customerId: String?,
        employeeId: String?,
        status: SaleStatus?,
        paymentStatus: PaymentStatus?
    ): List<Sale> = withContext(appDispatchers.io) {
        invoiceDao.observeAll().first()
            .toSalesListSafely()
            .asSequence()
            .filter { from == null || !it.createdAt.isBefore(from) }
            .filter { to == null || !it.createdAt.isAfter(to) }
            .filter { customerId == null || it.customerId == customerId }
            .filter { employeeId == null || it.employeeId == employeeId }
            .filter { status == null || it.saleStatus == status }
            .filter { paymentStatus == null || it.paymentStatus == paymentStatus }
            .toList()
    }

    override suspend fun searchSales(query: String): List<Sale> = withContext(appDispatchers.io) {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isBlank()) return@withContext emptyList()

        invoiceDao.observeAll().first()
            .toSalesListSafely()
            .filter { sale ->
                sale.invoiceNumber.contains(needle, ignoreCase = true) ||
                    sale.employeeId.contains(needle, ignoreCase = true) ||
                    (sale.customerId?.contains(needle, ignoreCase = true) == true) ||
                    (sale.note?.contains(needle, ignoreCase = true) == true) ||
                    sale.items.any { item ->
                        item.readString("productName", "displayName", "name")
                            .orEmpty()
                            .contains(needle, ignoreCase = true)
                    }
            }
    }

    override suspend fun saveSale(sale: Sale): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = invoiceDao.getById(sale.id)

            val localEntity = sale.toEntity(
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                updatedAtMillis = now
            )

            invoiceDao.insert(localEntity)

            val syncedEntity = syncToRemote(localEntity)
            syncedEntity.safeToSale() ?: sale
        }
    }

    override suspend fun saveSales(sales: List<Sale>): Result<List<Sale>> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()

            val entities = sales.map { sale ->
                val existing = invoiceDao.getById(sale.id)
                sale.toEntity(
                    serverId = existing?.serverId,
                    syncState = "PENDING",
                    isDeleted = existing?.isDeleted ?: false,
                    syncedAtMillis = existing?.syncedAtMillis,
                    updatedAtMillis = now
                )
            }

            invoiceDao.insertAll(entities)
            entities.mapNotNull { it.safeToSale() }
        }
    }

    override suspend fun updateSale(sale: Sale): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = invoiceDao.getById(sale.id)

            val localEntity = sale.toEntity(
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                updatedAtMillis = now
            )

            invoiceDao.update(localEntity)

            val syncedEntity = syncToRemote(localEntity)
            syncedEntity.safeToSale() ?: sale
        }
    }

    override suspend fun validateSale(sale: Sale): Result<SaleValidationResult> = withContext(appDispatchers.io) {
        runCatching {
            val totalAmount = sale.totalAmount
            SaleValidationResult(
                valid = sale.invoiceNumber.isNotBlank() &&
                    sale.employeeId.isNotBlank() &&
                    sale.items.isNotEmpty() &&
                    totalAmount >= BigDecimal.ZERO,
                saleId = sale.id,
                message = "تم التحقق من الفاتورة بنجاح",
                missingStockCount = 0,
                lowStockCount = 0,
                totalAmount = totalAmount
            )
        }
    }

    override suspend fun holdSale(sale: Sale): Result<SaleHold> = withContext(appDispatchers.io) {
        runCatching {
            SaleHold(
                holdId = UUID.randomUUID().toString(),
                saleId = sale.id,
                reason = "ON_HOLD",
                createdAtMillis = System.currentTimeMillis(),
                expiresAtMillis = null,
                metadata = mapOf("invoiceNumber" to sale.invoiceNumber)
            )
        }
    }

    override suspend fun resumeSale(holdId: String): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            throw UnsupportedOperationException(
                "resumeSale يحتاج جدولًا/DAO مخصصًا للحفظ المؤقت، وهو غير موجود في الملفات المتاحة."
            )
        }
    }

    override suspend fun cancelSale(saleId: String, reason: String?): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = invoiceDao.getById(saleId)
                ?: throw NoSuchElementException("Sale not found: $saleId")

            val updated = entity.copy(
                status = SaleStatus.CANCELLED.name,
                notes = reason?.takeIf { it.isNotBlank() } ?: entity.notes,
                syncState = "PENDING",
                updatedAtMillis = now
            )

            invoiceDao.update(updated)
            updated.safeToSale() ?: throw IllegalStateException("Failed to map sale: $saleId")
        }
    }

    override suspend fun markSaleCompleted(saleId: String): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = invoiceDao.getById(saleId)
                ?: throw NoSuchElementException("Sale not found: $saleId")

            val updated = entity.copy(
                status = SaleStatus.COMPLETED.name,
                syncState = "PENDING",
                updatedAtMillis = now
            )

            invoiceDao.update(updated)
            updated.safeToSale() ?: throw IllegalStateException("Failed to map sale: $saleId")
        }
    }

    override suspend fun markSaleRefunded(saleId: String): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = invoiceDao.getById(saleId)
                ?: throw NoSuchElementException("Sale not found: $saleId")

            val updated = entity.copy(
                status = SaleStatus.REFUNDED.name,
                syncState = "PENDING",
                updatedAtMillis = now
            )

            invoiceDao.update(updated)
            updated.safeToSale() ?: throw IllegalStateException("Failed to map sale: $saleId")
        }
    }

    override suspend fun applyDiscount(
        saleId: String,
        discountAmount: BigDecimal,
        reason: String?
    ): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = invoiceDao.getById(saleId)
                ?: throw NoSuchElementException("Sale not found: $saleId")

            val updated = entity.copy(
                discountAmount = discountAmount.money(),
                notes = reason?.takeIf { it.isNotBlank() } ?: entity.notes,
                syncState = "PENDING",
                updatedAtMillis = now
            )

            invoiceDao.update(updated)
            updated.safeToSale() ?: throw IllegalStateException("Failed to map sale: $saleId")
        }
    }

    override suspend fun getTotalSalesAmount(
        from: LocalDateTime?,
        to: LocalDateTime?
    ): BigDecimal = withContext(appDispatchers.io) {
        getSales(from = from, to = to)
            .fold(BigDecimal.ZERO) { acc, sale -> acc + sale.totalAmount }
    }

    override suspend fun getTopSellingProducts(
        limit: Int,
        from: LocalDateTime?,
        to: LocalDateTime?,
        sortDirection: SortDirection
    ): List<SaleProductRanking> = withContext(appDispatchers.io) {
        val sales = getSales(from = from, to = to)

        val grouped = sales
            .flatMap { sale -> sale.items.map { item -> sale to item } }
            .groupBy(
                keySelector = { (_, item) ->
                    item.readString(
                        "productBarcode",
                        "barcode",
                        "productId",
                        "id",
                        "productName",
                        "displayName",
                        "name"
                    ).orEmpty().ifBlank { "UNKNOWN" }
                }
            )
            .mapNotNull { (key, pairs) ->
                val firstItem = pairs.firstOrNull()?.second ?: return@mapNotNull null

                val productName = firstItem.readString(
                    "productName",
                    "displayName",
                    "name",
                    "productBarcode",
                    "barcode",
                    "productId"
                ).orEmpty().ifBlank { key }

                val quantitySold = pairs.fold(BigDecimal.ZERO) { acc, (_, item) ->
                    acc + (item.readBigDecimal("quantity") ?: BigDecimal.ONE)
                }

                val revenue = pairs.fold(BigDecimal.ZERO) { acc, (_, item) ->
                    val quantity = item.readBigDecimal("quantity") ?: BigDecimal.ONE
                    val unitPrice = item.readBigDecimal("unitPrice") ?: BigDecimal.ZERO
                    val taxAmount = item.readBigDecimal("taxAmount") ?: BigDecimal.ZERO
                    val discountAmount = item.readBigDecimal("discountAmount") ?: BigDecimal.ZERO
                    acc + (quantity * unitPrice) + taxAmount - discountAmount
                }

                SaleProductRanking(
                    productBarcode = key,
                    productName = productName,
                    quantitySold = quantitySold,
                    revenue = revenue,
                    rank = 0
                )
            }

        val sorted = when (sortDirection) {
            SortDirection.ASCENDING -> grouped.sortedBy { it.quantitySold }
            SortDirection.DESCENDING -> grouped.sortedByDescending { it.quantitySold }
        }

        sorted.take(limit.coerceAtLeast(0)).mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }

    override suspend fun getSalesPendingSync(): List<Sale> = withContext(appDispatchers.io) {
        invoiceDao.observePendingSync().first().toSalesListSafely()
    }

    override suspend fun markSaleSynced(saleId: String): Result<Sale> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = invoiceDao.getById(saleId)
                ?: throw NoSuchElementException("Sale not found: $saleId")

            val synced = entity.copy(
                syncState = "SYNCED",
                syncedAtMillis = now,
                updatedAtMillis = now
            )

            invoiceDao.update(synced)
            synced.safeToSale() ?: throw IllegalStateException("Failed to map sale: $saleId")
        }
    }

    override suspend fun deleteSale(id: String): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = invoiceDao.getById(id)

            if (entity != null) {
                invoiceDao.softDelete(
                    id = id,
                    updatedAtMillis = now,
                    syncState = if (entity.serverId.isNullOrBlank()) "PENDING" else "SYNCED"
                )

                entity.serverId?.let { serverId ->
                    remoteDataSource.deleteInvoice(serverId)
                }
            }

            Unit
        }
    }

    override suspend fun deleteSales(ids: List<String>): Result<Int> = withContext(appDispatchers.io) {
        runCatching { ids.count { deleteSale(it).isSuccess } }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            invoiceDao.observeAll().first().forEach { entity ->
                invoiceDao.delete(entity)
            }
            invoiceDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countSales(): Long = withContext(appDispatchers.io) {
        invoiceDao.observeAll().first().count { it.safeToSale() != null }.toLong()
    }

    override suspend fun countSalesByStatus(status: SaleStatus): Long = withContext(appDispatchers.io) {
        invoiceDao.observeAll().first()
            .toSalesListSafely()
            .count { it.saleStatus == status }
            .toLong()
    }

    override suspend fun countCompletedSales(): Long = withContext(appDispatchers.io) {
        invoiceDao.observeAll().first()
            .toSalesListSafely()
            .count { it.saleStatus == SaleStatus.COMPLETED }
            .toLong()
    }

    override suspend fun countPendingSales(): Long = withContext(appDispatchers.io) {
        invoiceDao.observeAll().first()
            .toSalesListSafely()
            .count { it.isPendingLike() }
            .toLong()
    }

    private suspend fun syncToRemote(localEntity: InvoiceEntity): InvoiceEntity {
        val sale = localEntity.safeToSale() ?: return localEntity

        val request = sale.toDto(
            serverId = localEntity.serverId,
            syncState = "PENDING",
            isDeleted = localEntity.isDeleted,
            syncedAtMillis = localEntity.syncedAtMillis,
            createdAtMillis = localEntity.createdAtMillis,
            updatedAtMillis = localEntity.updatedAtMillis
        )

        val remoteResult = if (localEntity.serverId.isNullOrBlank()) {
            remoteDataSource.createInvoice(request)
        } else {
            remoteDataSource.updateInvoice(localEntity.serverId, request)
        }

        return when (remoteResult) {
            is NetworkResult.Success -> {
                val remoteServerId = remoteResult.data.id ?: localEntity.serverId ?: localEntity.id
                val synced = localEntity.copy(
                    serverId = remoteServerId,
                    syncState = "SYNCED",
                    syncedAtMillis = System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )
                invoiceDao.update(synced)
                synced
            }

            is NetworkResult.Error -> localEntity
            NetworkResult.Loading -> localEntity
        }
    }

    private fun InvoiceEntity.safeToSale(): Sale? {
        return runCatching { toSale() }.getOrNull()
    }

    private fun List<InvoiceEntity>.toSalesListSafely(): List<Sale> {
        return mapNotNull { it.safeToSale() }
    }

    private fun InvoiceEntity.toSale(): Sale {
        val parsedItems = linesJson.toSaleItemList()
        require(parsedItems.isNotEmpty()) { "linesJson produced an empty items list." }

        return Sale(
            id = id,
            invoiceNumber = invoiceNumber,
            customerId = partyId,
            employeeId = issuedByEmployeeId.orEmpty(),
            items = parsedItems,
            paidAmount = paidAmount.toBigDecimalOrZero(),
            saleStatus = runCatching { SaleStatus.valueOf(status.trim().uppercase(Locale.ROOT)) }
                .getOrDefault(SaleStatus.COMPLETED),
            createdAt = issueDateMillis.toLocalDateTime(),
            note = notes
        )
    }

    private fun Sale.toEntity(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): InvoiceEntity {
        return InvoiceEntity(
            id = id,
            serverId = serverId,
            invoiceNumber = invoiceNumber,
            invoiceType = "SALE",
            status = saleStatus.name,
            paymentStatus = paymentStatus.name,
            issueDateMillis = createdAt.toMillis(),
            dueDateMillis = null,
            partyId = customerId,
            partyName = null,
            partyTaxNumber = null,
            issuedByEmployeeId = employeeId,
            linesJson = items.toJsonString(),
            taxAmount = taxAmount.money(),
            discountAmount = discountAmount.money(),
            paidAmount = paidAmount.money(),
            notes = note,
            termsAndConditions = null,
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            createdAtMillis = createdAt.toMillis(),
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun Sale.toDto(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = createdAt.toMillis(),
        updatedAtMillis: Long = System.currentTimeMillis()
    ): SaleInvoiceDto {
        return SaleInvoiceDto(
            id = serverId ?: this.id,
            invoiceNumber = invoiceNumber,
            saleId = id,
            status = saleStatus.name,
            issueDate = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            dueDate = null,
            taxReference = null,
            customerSnapshot = null,
            employeeId = employeeId,
            items = items.toSaleItemDtoList(),
            paidAmount = paidAmount.money(),
            notes = note,
            termsAndConditions = null,
            qrPayload = null
        )
    }

    private fun List<SaleItem>.toJsonString(): String {
        return JsonArray().apply {
            forEach { item ->
                add(gson.toJsonTree(item))
            }
        }.toString()
    }

    private fun String.toSaleItemList(): List<SaleItem> {
        if (isBlank()) return emptyList()

        return runCatching {
            val array = gson.fromJson(this, JsonArray::class.java) ?: JsonArray()
            buildList {
                for (i in 0 until array.size()) {
                    val element = array.get(i)
                    val normalized = normalizeSaleItemElement(element)
                    val item = gson.fromJson(normalized, SaleItem::class.java)
                    if (item != null) add(item)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun normalizeSaleItemElement(element: JsonElement): JsonElement {
        val source = runCatching { element.asJsonObject }.getOrNull() ?: JsonObject()
        val target = JsonObject()
        val targetFields = SaleItem::class.java.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }

        for (field in targetFields) {
            val fieldName = field.name
            val fieldType = field.type

            when (fieldName.lowercase(Locale.ROOT)) {
                "id" -> target.addProperty(fieldName, source.readString("id") ?: UUID.randomUUID().toString())
                "saleid" -> target.addProperty(fieldName, source.readString("saleId") ?: "")
                "productid" -> target.addProperty(fieldName, source.readString("productId", "product_id") ?: "")
                "barcode", "productbarcode" -> target.addProperty(fieldName, source.readString("barcode", "productBarcode", "product_barcode") ?: "")
                "productname", "name" -> target.addProperty(fieldName, source.readString("productName", "product_name", "displayName", "display_name") ?: "")
                "displayname" -> target.addProperty(fieldName, source.readString("displayName", "display_name", "productName", "product_name") ?: "")
                "unit", "unitofmeasure" -> target.addProperty(fieldName, source.readString("unit", "unitOfMeasure", "unit_of_measure") ?: "UNIT")
                "quantity" -> target.addProperty(fieldName, source.readBigDecimal("quantity")?.money() ?: "1.00")
                "unitprice" -> target.addProperty(fieldName, source.readBigDecimal("unitPrice", "unit_price")?.money() ?: "0.00")
                "taxamount" -> target.addProperty(fieldName, source.readBigDecimal("taxAmount", "tax_amount")?.money() ?: "0.00")
                "discountamount" -> target.addProperty(fieldName, source.readBigDecimal("discountAmount", "discount_amount")?.money() ?: "0.00")
                "isreturn" -> target.addProperty(fieldName, source.readBoolean("isReturn", "is_return") ?: false)
                "note" -> target.addProperty(fieldName, source.readString("note") ?: "")
                else -> when {
                    fieldType == String::class.java -> target.addProperty(fieldName, source.readString(fieldName) ?: "")
                    fieldType == java.math.BigDecimal::class.java -> target.addProperty(
                        fieldName,
                        source.readBigDecimal(fieldName)?.money() ?: "0.00"
                    )
                    fieldType == Boolean::class.java || fieldType == java.lang.Boolean::class.java -> target.addProperty(
                        fieldName,
                        source.readBoolean(fieldName) ?: false
                    )
                    fieldType.isEnum -> target.addProperty(fieldName, source.readString(fieldName) ?: fieldType.enumConstants.first().toString())
                    else -> {
                        val anyValue = source.get(fieldName)
                        if (anyValue != null && !anyValue.isJsonNull) {
                            target.add(fieldName, anyValue)
                        }
                    }
                }
            }
        }

        if (target.size() == 0) {
            target.addProperty("id", UUID.randomUUID().toString())
            target.addProperty("productName", "Sale Item")
            target.addProperty("quantity", "1.00")
            target.addProperty("unitPrice", "0.00")
            target.addProperty("taxAmount", "0.00")
            target.addProperty("discountAmount", "0.00")
        }

        return target
    }

    private fun List<SaleItem>.toSaleItemDtoList(): List<SaleItemDto> {
        return map { item ->
            SaleItemDto(
                id = item.readString("id") ?: UUID.randomUUID().toString(),
                productId = item.readString("productId", "product_id"),
                productName = item.readString("productName", "product_name", "displayName", "display_name"),
                quantity = item.readBigDecimal("quantity")?.money(),
                unitPrice = item.readBigDecimal("unitPrice", "unit_price")?.money(),
                taxAmount = item.readBigDecimal("taxAmount", "tax_amount")?.money(),
                discountAmount = item.readBigDecimal("discountAmount", "discount_amount")?.money()
            )
        }
    }

    private fun Any.readString(vararg keys: String): String? {
        val objectTree = runCatching { gson.toJsonTree(this).asJsonObject }.getOrNull() ?: return null
        for (key in keys) {
            val value = objectTree.get(key)
            if (value != null && !value.isJsonNull) {
                val text = runCatching { value.asString }.getOrNull()
                if (!text.isNullOrBlank()) return text
            }
        }
        return null
    }

    private fun Any.readBigDecimal(vararg keys: String): BigDecimal? {
        val objectTree = runCatching { gson.toJsonTree(this).asJsonObject }.getOrNull() ?: return null
        for (key in keys) {
            val value = objectTree.get(key)
            if (value != null && !value.isJsonNull) {
                val parsed = runCatching {
                    when {
                        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asBigDecimal
                        value.isJsonPrimitive && value.asJsonPrimitive.isString -> BigDecimal(value.asString.trim())
                        else -> null
                    }
                }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun Any.readBoolean(vararg keys: String): Boolean? {
        val objectTree = runCatching { gson.toJsonTree(this).asJsonObject }.getOrNull() ?: return null
        for (key in keys) {
            val value = objectTree.get(key)
            if (value != null && !value.isJsonNull) {
                val parsed = runCatching {
                    when {
                        value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
                        value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString.toBooleanStrictOrNull()
                        else -> null
                    }
                }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun JsonObject.readString(vararg keys: String): String? {
        for (key in keys) {
            val value = get(key)
            if (value != null && !value.isJsonNull) {
                val text = runCatching { value.asString }.getOrNull()
                if (!text.isNullOrBlank()) return text
            }
        }
        return null
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(trim()) }
            .getOrDefault(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.money(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
    }

    private fun Sale.isPendingLike(): Boolean {
        return saleStatus != SaleStatus.COMPLETED || !isFullyPaid()
    }
}