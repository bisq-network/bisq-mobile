package network.bisq.mobile.presentation.ui.error

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.setupUncaughtExceptionHandler
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.getLogger
import kotlin.jvm.JvmStatic

class GenericErrorHandler : Logging {
    companion object {
        private val _isUncaughtException: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val isUncaughtException: StateFlow<Boolean> get() = _isUncaughtException

        private val _genericErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
        val genericErrorMessage: StateFlow<String?> get() = _genericErrorMessage

        fun clearGenericError() {
            _genericErrorMessage.value = null
        }

        fun handleGenericError(value: String?) {
            _genericErrorMessage.value = value
        }

        fun handleGenericError(errorMessage: String, exception: Throwable) {
            getLogger("GenericErrorHandler").e(errorMessage, exception)
            handleGenericError(errorMessage + "\nException: " + exception.message)
        }

        @JvmStatic
        fun init() {
            setupUncaughtExceptionHandler { throwable ->
                _isUncaughtException.value = true
                // this ensures compose can observe the change.
                MainScope().launch {
                    handleGenericError("Application stopped unexpectedly", throwable)
                }
            }
        }
    }
}