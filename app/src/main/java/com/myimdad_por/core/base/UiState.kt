package com.myimdad_por.core.base

/**
 * حالة عامة للواجهة (UI State)
 * مناسبة للاستخدام مع MVVM / Clean Architecture
 */
sealed interface UiState<out T> {

    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>

    data class Success<out T>(
        val data: T
    ) : UiState<T>

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val code: Int? = null
    ) : UiState<Nothing>
}

/**
 * دوال مساعدة للتعامل مع UiState بشكل نظيف
 */

inline fun <T, R> UiState<T>.fold(
    onIdle: () -> R,
    onLoading: () -> R,
    onEmpty: () -> R,
    onSuccess: (T) -> R,
    onError: (UiState.Error) -> R
): R = when (this) {
    UiState.Idle -> onIdle()
    UiState.Loading -> onLoading()
    UiState.Empty -> onEmpty()
    is UiState.Success -> onSuccess(data)
    is UiState.Error -> onError(this)
}

inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) action(data)
    return this
}

inline fun <T> UiState<T>.onError(action: (UiState.Error) -> Unit): UiState<T> {
    if (this is UiState.Error) action(this)
    return this
}

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}

inline fun <T> UiState<T>.onEmpty(action: () -> Unit): UiState<T> {
    if (this is UiState.Empty) action()
    return this
}

inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        UiState.Idle -> UiState.Idle
        UiState.Loading -> UiState.Loading
        UiState.Empty -> UiState.Empty
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Error -> this
    }
}

fun <T> UiState<T>.getOrNull(): T? = (this as? UiState.Success)?.data

fun <T> UiState<T>.exceptionOrNull(): Throwable? = (this as? UiState.Error)?.throwable

fun <T> UiState<T>.messageOrNull(): String? = (this as? UiState.Error)?.message

val UiState<*>.isIdle: Boolean
    get() = this is UiState.Idle

val UiState<*>.isLoading: Boolean
    get() = this is UiState.Loading

val UiState<*>.isEmpty: Boolean
    get() = this is UiState.Empty

val UiState<*>.isSuccess: Boolean
    get() = this is UiState.Success<*>

val UiState<*>.isError: Boolean
    get() = this is UiState.Error