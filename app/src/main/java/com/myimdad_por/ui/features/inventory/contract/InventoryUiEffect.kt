package com.myimdad_por.ui.features.inventory.contract

import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.ui.navigation.ScreenRoutes

/**
 * One-time side effects for the inventory feature.
 *
 * Effects are intentionally separate from state so navigation and transient UI
 * actions do not get replayed when the screen recomposes or re-observes state.
 */
sealed interface InventoryUiEffect : UiEvent {

    data class ShowMessage(
        val message: String
    ) : InventoryUiEffect

    data class ShowError(
        val message: String,
        val throwable: Throwable? = null
    ) : InventoryUiEffect

    data object HideKeyboard : InventoryUiEffect
    data object NavigateBack : InventoryUiEffect
    data object Finish : InventoryUiEffect

    data class NavigateToProductDetails(
        val barcode: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : InventoryUiEffect {
        fun route(): String = "${ScreenRoutes.ProductDetailsPattern.removeSuffix("/{${ScreenRoutes.ARG_ID}}")}/$barcode"
    }

    data class NavigateToStockMovement(
        val barcode: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : InventoryUiEffect {
        fun route(): String = "${ScreenRoutes.StockMovement}/$barcode"
    }

    data class NavigateToInventory(
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = true
    ) : InventoryUiEffect {
        fun route(): String = ScreenRoutes.Inventory
    }

    data class NavigateToProductCreate(
        val barcode: String? = null,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : InventoryUiEffect {
        fun route(): String = buildString {
            append(ScreenRoutes.ProductCreate)
            barcode?.takeIf { it.isNotBlank() }?.let { append("?${ScreenRoutes.ARG_ID}=").append(it) }
        }
    }

    data class NavigateToSearch(
        val query: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : InventoryUiEffect {
        fun route(): String = ScreenRoutes.search(query)
    }

    data class NavigateToRoute(
        val route: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : InventoryUiEffect

    data object RequestRefresh : InventoryUiEffect
    data object ClearSelection : InventoryUiEffect
    data object CloseForm : InventoryUiEffect
}
