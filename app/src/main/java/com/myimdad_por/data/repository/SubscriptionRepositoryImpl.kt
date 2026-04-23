package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.remote.datasource.SubscriptionRemoteDataSource
import com.myimdad_por.data.remote.dto.SubscriptionResponseDto
import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.domain.repository.SubscriptionRepository
import com.myimdad_por.domain.repository.SubscriptionStatusSummary
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SubscriptionRepositoryImpl @Inject constructor(
    private val remoteDataSource: SubscriptionRemoteDataSource
) : SubscriptionRepository {

    private val ioDispatcher = DefaultAppDispatchers.io
    private val mutex = Mutex()

    private val subscriptionState = MutableStateFlow<SubscriptionInfo?>(null)
    private val usageState = MutableStateFlow(UsageState())

    override fun observeSubscription(): Flow<SubscriptionInfo?> {
        return subscriptionState.asStateFlow()
    }

    override fun observeSubscriptionStatus(): Flow<SubscriptionStatus?> {
        return subscriptionState
            .asStateFlow()
            .map { it?.status }
            .distinctUntilChanged()
    }

    override suspend fun getSubscription(): SubscriptionInfo? = withContext(ioDispatcher) {
        currentSubscription() ?: refreshSubscriptionFromServer().getOrNull()
    }

    override suspend fun refreshSubscriptionFromServer(): Result<SubscriptionInfo> = withContext(ioDispatcher) {
        runCatching {
            val remote = remoteDataSource.getCurrentSubscription().orThrow()
            remote.toDomain().also { updateLocalSubscription(it) }
        }
    }

    override suspend fun saveSubscription(subscriptionInfo: SubscriptionInfo): Result<SubscriptionInfo> =
        withContext(ioDispatcher) {
            runCatching {
                val saved = remoteDataSource
                    .updateSubscription(
                        subscriptionInfo.subscriptionId,
                        subscriptionInfo.toDto()
                    )
                    .orThrow()
                    .toDomain()

                updateLocalSubscription(saved)
                saved
            }
        }

    override suspend fun clearSubscription(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            mutex.withLock {
                subscriptionState.value = null
                usageState.value = UsageState()
            }
            Unit
        }
    }

    override suspend fun isSubscriptionActive(): Boolean = withContext(ioDispatcher) {
        currentSubscription()?.isActive == true
    }

    override suspend fun isSubscriptionExpired(): Boolean = withContext(ioDispatcher) {
        currentSubscription()?.isExpired ?: true
    }

    override suspend fun isInGracePeriod(): Boolean = withContext(ioDispatcher) {
        currentSubscription()?.isInGracePeriod == true
    }

    override suspend fun canOperateOffline(): Boolean = withContext(ioDispatcher) {
        currentSubscription()?.canOperateOffline == true
    }

    override suspend fun canUsePaidFeatures(): Boolean = withContext(ioDispatcher) {
        currentSubscription()?.canUsePaidFeatures() == true
    }

    override suspend fun checkFeatureAccess(feature: SubscriptionFeature): Boolean = withContext(ioDispatcher) {
        val current = currentSubscription() ?: return@withContext false
        if (!current.canUsePaidFeatures()) return@withContext false
        current.featuresEnabled.contains(feature) || feature in alwaysAllowedFeatures()
    }

    override suspend fun canAccessInvoices(): Boolean = withContext(ioDispatcher) {
        if (!canUsePaidFeatures()) return@withContext false
        val remaining = getRemainingInvoicesCount()
        remaining == null || remaining > 0
    }

    override suspend fun canAccessReports(): Boolean = withContext(ioDispatcher) {
        if (!canUsePaidFeatures()) return@withContext false

        checkFeatureAccess(SubscriptionFeature.ADVANCED_REPORTS) ||
            checkFeatureAccess(SubscriptionFeature.EXPORT_EXCEL) ||
            checkFeatureAccess(SubscriptionFeature.EXPORT_PDF)
    }

    override suspend fun isReadOnlyMode(): Boolean = withContext(ioDispatcher) {
        !canUsePaidFeatures()
    }

    override suspend fun getAllowedPlan(): SubscriptionPlan? = withContext(ioDispatcher) {
        currentSubscription()?.plan
    }

    override suspend fun getRemainingUsersCount(): Int? = withContext(ioDispatcher) {
        currentSubscription()?.maxUsers
    }

    override suspend fun getRemainingInvoicesCount(): Int? = withContext(ioDispatcher) {
        val current = currentSubscription() ?: return@withContext null
        val limit = current.maxInvoicesPerMonth ?: return@withContext null
        val used = usageState.value.invoiceUsageCount
        (limit - used).coerceAtLeast(0)
    }

    override suspend fun recordInvoiceUsage(count: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            require(count > 0) { "count must be greater than zero." }

            val current = currentSubscription()
                ?: throw IllegalStateException("No subscription is loaded.")

            val limit = current.maxInvoicesPerMonth

            mutex.withLock {
                val nextCount = usageState.value.invoiceUsageCount + count
                if (limit != null && nextCount > limit) {
                    throw IllegalStateException("Invoice limit exceeded.")
                }
                usageState.value = usageState.value.copy(invoiceUsageCount = nextCount)
            }

            Unit
        }
    }

    override suspend fun recordFeatureUsage(feature: SubscriptionFeature): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            mutex.withLock {
                val current = usageState.value
                val nextCounts = current.featureUsageCounts + (
                    feature to ((current.featureUsageCounts[feature] ?: 0) + 1)
                )
                usageState.value = current.copy(featureUsageCounts = nextCounts)
            }
            Unit
        }
    }

    override suspend fun syncUsageWithServer(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val current = currentSubscription()
                ?: throw IllegalStateException("No subscription is loaded.")

            val updated = remoteDataSource
                .updateSubscription(current.subscriptionId, current.toDto())
                .orThrow()
                .toDomain()

            updateLocalSubscription(updated)
            Unit
        }
    }

    override suspend fun getSubscriptionStatusSummary(): SubscriptionStatusSummary =
        withContext(ioDispatcher) {
            val current = currentSubscription()

            SubscriptionStatusSummary(
                subscriptionId = current?.subscriptionId,
                plan = current?.plan,
                status = current?.status,
                isActive = current?.isActive ?: false,
                isExpired = current?.isExpired ?: true,
                isInGracePeriod = current?.isInGracePeriod ?: false,
                canOperateOffline = current?.canOperateOffline ?: false,
                canUsePaidFeatures = current?.canUsePaidFeatures() ?: false,
                canAccessInvoices = current?.let {
                    it.canUsePaidFeatures() &&
                        (it.maxInvoicesPerMonth == null || it.maxInvoicesPerMonth > 0)
                } ?: false,
                canAccessReports = current?.let {
                    it.canUsePaidFeatures() &&
                        (
                            it.featuresEnabled.contains(SubscriptionFeature.ADVANCED_REPORTS) ||
                                it.featuresEnabled.contains(SubscriptionFeature.EXPORT_EXCEL) ||
                                it.featuresEnabled.contains(SubscriptionFeature.EXPORT_PDF)
                        )
                } ?: false,
                readOnlyMode = current?.let { !it.canUsePaidFeatures() } ?: true,
                remainingUsers = current?.maxUsers,
                remainingInvoices = current?.let {
                    val limit = it.maxInvoicesPerMonth
                    if (limit == null) null else (limit - usageState.value.invoiceUsageCount).coerceAtLeast(0)
                }
            )
        }

    private suspend fun currentSubscription(): SubscriptionInfo? = mutex.withLock {
        subscriptionState.value
    }

    private suspend fun updateLocalSubscription(subscriptionInfo: SubscriptionInfo) {
        mutex.withLock {
            subscriptionState.value = subscriptionInfo
        }
    }

    private fun <T> NetworkResult<T>.orThrow(): T {
        return when (this) {
            is NetworkResult.Success -> data
            is NetworkResult.Error -> throw exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
        }
    }

    private fun SubscriptionResponseDto.toDomain(): SubscriptionInfo {
        return SubscriptionInfo(
            subscriptionId = subscriptionId,
            plan = parsePlan(plan),
            status = parseStatus(status),
            startDateMillis = startDateMillis,
            expiryDateMillis = expiryDateMillis,
            maxUsers = maxUsers,
            maxInvoicesPerMonth = maxInvoicesPerMonth,
            featuresEnabled = featuresEnabled.mapNotNull { parseFeature(it) }.toSet(),
            offlineGracePeriodDays = offlineGracePeriodDays,
            lastSyncedAtMillis = lastSyncedAtMillis,
            renewedAtMillis = renewedAtMillis,
            notes = notes,
            metadata = metadataJson.toStringMap()
        )
    }

    private fun SubscriptionInfo.toDto(): SubscriptionResponseDto {
        return SubscriptionResponseDto(
            subscriptionId = subscriptionId,
            plan = plan.name,
            status = status.name,
            startDateMillis = startDateMillis,
            expiryDateMillis = expiryDateMillis,
            maxUsers = maxUsers,
            maxInvoicesPerMonth = maxInvoicesPerMonth,
            featuresEnabled = featuresEnabled.map { it.name },
            offlineGracePeriodDays = offlineGracePeriodDays,
            lastSyncedAtMillis = lastSyncedAtMillis,
            renewedAtMillis = renewedAtMillis,
            notes = notes,
            metadataJson = metadata.toJsonString()
        )
    }

    private fun parsePlan(value: String): SubscriptionPlan {
        return runCatching {
            SubscriptionPlan.valueOf(value.trim().uppercase(Locale.ROOT))
        }.getOrDefault(SubscriptionPlan.CUSTOM)
    }

    private fun parseStatus(value: String): SubscriptionStatus {
        return runCatching {
            SubscriptionStatus.valueOf(value.trim().uppercase(Locale.ROOT))
        }.getOrDefault(SubscriptionStatus.PENDING)
    }

    private fun parseFeature(value: String): SubscriptionFeature? {
        return runCatching {
            SubscriptionFeature.valueOf(value.trim().uppercase(Locale.ROOT))
        }.getOrNull()
    }

    private fun String.toStringMap(): Map<String, String> {
        if (isBlank()) return emptyMap()

        return runCatching {
            val json = JSONObject(this)
            buildMap {
                json.keys().forEach { key ->
                    put(key, json.optString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun Map<String, String>.toJsonString(): String {
        return JSONObject(this).toString()
    }

    private fun SubscriptionInfo.canUsePaidFeatures(): Boolean {
        return isActive || isInGracePeriod
    }

    private fun alwaysAllowedFeatures(): Set<SubscriptionFeature> {
        return setOf(SubscriptionFeature.OFFLINE_MODE)
    }

    private data class UsageState(
        val invoiceUsageCount: Int = 0,
        val featureUsageCounts: Map<SubscriptionFeature, Int> = emptyMap()
    )
}