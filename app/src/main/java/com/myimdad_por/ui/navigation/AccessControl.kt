package com.myimdad_por.ui.navigation

import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.repository.SubscriptionRepository
import com.myimdad_por.domain.repository.SubscriptionStatusSummary

enum class AccessRequirement {
    Public,
    Authentication,
    SubscriptionActive,
    SubscriptionActiveOrGrace,
    SubscriptionManagement
}

data class AccessDecision(
    val allowed: Boolean,
    val fallbackRoute: String? = null,
    val reason: String = "none"
) {
    companion object {
        fun allowed() = AccessDecision(allowed = true)
        fun blocked(fallbackRoute: String, reason: String) =
            AccessDecision(allowed = false, fallbackRoute = fallbackRoute, reason = reason)
    }
}

object AccessControl {

    private val publicRoutes = setOf(
        ScreenRoutes.Splash,
        ScreenRoutes.Onboarding,
        ScreenRoutes.Login,
        ScreenRoutes.Register,
        ScreenRoutes.ForgotPassword
    )

    private val subscriptionManagementRoutes = setOf(
        ScreenRoutes.Subscription,
        ScreenRoutes.SubscriptionExpired,
        ScreenRoutes.SubscriptionRenew
    )

    private val protectedModulePrefixes = listOf(
        ScreenRoutes.Home,
        ScreenRoutes.Dashboard,
        ScreenRoutes.Profile,
        ScreenRoutes.Settings,
        ScreenRoutes.Notifications,
        ScreenRoutes.Search,
        ScreenRoutes.Customers,
        ScreenRoutes.Suppliers,
        ScreenRoutes.Products,
        ScreenRoutes.Inventory,
        ScreenRoutes.Invoices,
        ScreenRoutes.Purchases,
        ScreenRoutes.Sales,
        ScreenRoutes.Payments,
        ScreenRoutes.Returns,
        ScreenRoutes.Expenses,
        ScreenRoutes.Reports,
        ScreenRoutes.Analytics,
        ScreenRoutes.AuditLogs
    )

    fun classify(route: String): AccessRequirement {
        return when {
            route in publicRoutes -> AccessRequirement.Public
            route in subscriptionManagementRoutes -> AccessRequirement.SubscriptionManagement
            requiresSubscription(route) -> AccessRequirement.SubscriptionActive
            else -> AccessRequirement.Authentication
        }
    }

    fun resolveStartDestination(
        isLoggedIn: Boolean,
        hasActiveSubscription: Boolean,
        isInGracePeriod: Boolean = false
    ): String {
        return when {
            !isLoggedIn -> ScreenRoutes.Splash
            hasActiveSubscription || isInGracePeriod -> ScreenRoutes.Home
            else -> ScreenRoutes.SubscriptionExpired
        }
    }

    suspend fun canEnterRoute(
        route: String,
        subscriptionRepository: SubscriptionRepository? = null
    ): AccessDecision {
        if (route in publicRoutes) {
            return AccessDecision.allowed()
        }

        val sessionValid = isSessionValid()
        if (!sessionValid) {
            return AccessDecision.blocked(
                fallbackRoute = ScreenRoutes.Login,
                reason = "session_missing"
            )
        }

        if (route in subscriptionManagementRoutes) {
            return AccessDecision.allowed()
        }

        if (!requiresSubscription(route)) {
            return AccessDecision.allowed()
        }

        val summary = subscriptionRepository?.getSubscriptionStatusSummary()
        return if (summary == null) {
            AccessDecision.blocked(
                fallbackRoute = ScreenRoutes.SubscriptionExpired,
                reason = "subscription_unknown"
            )
        } else if (summary.isActive || summary.isInGracePeriod || summary.canOperateOffline) {
            AccessDecision.allowed()
        } else {
            AccessDecision.blocked(
                fallbackRoute = ScreenRoutes.SubscriptionExpired,
                reason = "subscription_inactive"
            )
        }
    }

    suspend fun canAccessFeature(
        feature: SubscriptionFeature,
        subscriptionRepository: SubscriptionRepository
    ): Boolean {
        return runCatching {
            subscriptionRepository.checkFeatureAccess(feature)
        }.getOrDefault(false)
    }

    suspend fun shouldShowExpiredDialog(
        subscriptionRepository: SubscriptionRepository
    ): Boolean {
        val summary = subscriptionRepository.getSubscriptionStatusSummary()
        return !summary.isActive && !summary.isInGracePeriod
    }

    suspend fun getBlockedReason(
        subscriptionRepository: SubscriptionRepository
    ): String {
        val summary = subscriptionRepository.getSubscriptionStatusSummary()
        return when {
            !isSessionValid() -> "session_missing"
            !summary.isActive && !summary.isInGracePeriod -> "subscription_expired"
            summary.readOnlyMode -> "read_only_mode"
            else -> "none"
        }
    }

    suspend fun getSubscriptionSummary(
        subscriptionRepository: SubscriptionRepository
    ): SubscriptionStatusSummary {
        return subscriptionRepository.getSubscriptionStatusSummary()
    }

    private fun requiresSubscription(route: String): Boolean {
        return protectedModulePrefixes.any { prefix ->
            route == prefix || route.startsWith("$prefix/")
        }
    }

    private fun isSessionValid(): Boolean {
        return runCatching {
            SessionManager.hasValidSession()
        }.getOrDefault(false)
    }
}