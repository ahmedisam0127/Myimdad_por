package com.myimdad_por.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BaseViewModel عام لإدارة:
 * - حالة الواجهة UI State
 * - الأحداث اللحظية UI Events
 * - تنفيذ العمليات عبر Dispatchers قابلة للحقن
 *
 * @param S نوع البيانات داخل UiState.Success
 * @param dispatchers مزود الـ Dispatchers الخاص بالتطبيق
 */
abstract class BaseViewModel<S>(
    protected val dispatchers: AppDispatchers = DefaultAppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<S>>(UiState.Idle)
    val uiState: StateFlow<UiState<S>> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    /**
     * الحالة الحالية مباشرة
     */
    protected val currentState: UiState<S>
        get() = _uiState.value

    /**
     * البيانات الحالية إن كانت الحالة Success
     */
    protected val currentData: S?
        get() = (currentState as? UiState.Success)?.data

    /**
     * تغيير الحالة بالكامل
     */
    protected fun setState(state: UiState<S>) {
        _uiState.value = state
    }

    /**
     * تحديث الحالة بشكل آمن
     */
    protected fun updateState(reducer: (UiState<S>) -> UiState<S>) {
        _uiState.update(reducer)
    }

    /**
     * تعيين حالة التحميل
     */
    protected fun setLoading() {
        _uiState.value = UiState.Loading
    }

    /**
     * تعيين حالة النجاح
     */
    protected fun setSuccess(data: S) {
        _uiState.value = UiState.Success(data)
    }

    /**
     * تعيين حالة الخطأ
     */
    protected fun setError(
        message: String,
        throwable: Throwable? = null,
        code: Int? = null
    ) {
        _uiState.value = UiState.Error(
            message = message,
            throwable = throwable,
            code = code
        )
    }

    /**
     * تعيين حالة فارغة
     */
    protected fun setEmpty() {
        _uiState.value = UiState.Empty
    }

    /**
     * تعيين الحالة إلى Idle
     */
    protected fun setIdle() {
        _uiState.value = UiState.Idle
    }

    /**
     * مسح الخطأ الحالي والعودة إلى Idle
     */
    protected fun clearError() {
        if (currentState is UiState.Error) {
            setIdle()
        }
    }

    /**
     * إرسال حدث لحظي للواجهة
     */
    protected fun sendEvent(event: UiEvent) {
        if (!_uiEvent.tryEmit(event)) {
            viewModelScope.launch(dispatchers.main) {
                _uiEvent.emit(event)
            }
        }
    }

    /**
     * تشغيل Coroutine داخل viewModelScope مع معالجة الأخطاء بشكل منظم
     */
    protected fun launch(
        dispatcher: CoroutineDispatcher = dispatchers.main,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(dispatcher) {
            try {
                block()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                onError?.invoke(throwable)
            }
        }
    }

    /**
     * تنفيذ عملية تُرجع بيانات مع تشغيلها غالبًا على dispatcher مناسب للـ IO
     */
    protected fun launchRequest(
        loading: Boolean = true,
        dispatcher: CoroutineDispatcher = dispatchers.io,
        onErrorMessage: String = "حدث خطأ غير متوقع",
        block: suspend () -> S
    ): Job {
        return launch(
            dispatcher = dispatchers.main,
            onError = { throwable ->
                setError(
                    message = throwable.message ?: onErrorMessage,
                    throwable = throwable
                )
            }
        ) {
            if (loading) setLoading()
            val result = withContext(dispatcher) {
                block()
            }
            setSuccess(result)
        }
    }

    /**
     * تنفيذ عملية لا تُرجع بيانات
     */
    protected fun launchAction(
        loading: Boolean = true,
        dispatcher: CoroutineDispatcher = dispatchers.io,
        onSuccessEvent: UiEvent? = null,
        onErrorMessage: String = "حدث خطأ غير متوقع",
        block: suspend () -> Unit
    ): Job {
        return launch(
            dispatcher = dispatchers.main,
            onError = { throwable ->
                setError(
                    message = throwable.message ?: onErrorMessage,
                    throwable = throwable
                )
            }
        ) {
            if (loading) setLoading()
            withContext(dispatcher) {
                block()
            }
            onSuccessEvent?.let { sendEvent(it) }
            setIdle()
        }
    }

    /**
     * تشغيل عملية مع تحكم يدوي كامل بالحالات
     */
    protected fun launchStateful(
        dispatcher: CoroutineDispatcher = dispatchers.io,
        onErrorMessage: String = "حدث خطأ غير متوقع",
        block: suspend () -> UiState<S>
    ): Job {
        return launch(
            dispatcher = dispatchers.main,
            onError = { throwable ->
                setError(
                    message = throwable.message ?: onErrorMessage,
                    throwable = throwable
                )
            }
        ) {
            setLoading()
            val newState = withContext(dispatcher) {
                block()
            }
            setState(newState)
        }
    }
}