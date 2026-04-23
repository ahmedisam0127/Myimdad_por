package com.myimdad_por.core.base

/**
 * يمثل أحداث الواجهة التي تحدث مرة واحدة فقط
 * مثل:
 * - التنقل بين الشاشات
 * - عرض رسالة
 * - إغلاق الشاشة
 * - فتح رابط خارجي
 */
sealed interface UiEvent {

    data class ShowMessage(
        val message: String
    ) : UiEvent

    data class ShowError(
        val message: String,
        val throwable: Throwable? = null
    ) : UiEvent

    data class NavigateTo(
        val route: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : UiEvent

    data object NavigateBack : UiEvent

    data class OpenUrl(
        val url: String
    ) : UiEvent

    data object HideKeyboard : UiEvent

    data object Finish : UiEvent
}