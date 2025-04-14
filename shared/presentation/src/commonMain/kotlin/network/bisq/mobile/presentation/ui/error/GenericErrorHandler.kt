package network.bisq.mobile.presentation.ui.error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.setupUncaughtExceptionHandler
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.getLogger
import kotlin.jvm.JvmStatic

class GenericErrorHandler : Logging {
    companion object {
        private val _isUncaughtException: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val isUncaughtException = _isUncaughtException

        private val _genericErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
        val genericErrorMessage: StateFlow<String?> = _genericErrorMessage

        fun clearGenericError() {
            _genericErrorMessage.value = null
        }

        fun handleGenericError(value: String?) {
            _genericErrorMessage.value = value
        }

        fun handleGenericError(exception: Throwable) {
            getLogger("GenericErrorHandler").e("", exception)
            _genericErrorMessage.value = "Exception: " + exception.message
        }

        fun handleGenericError(errorMessage: String, exception: Throwable) {
            getLogger("GenericErrorHandler").e(errorMessage, exception)
            _genericErrorMessage.value = errorMessage + "\nException: " + exception.message
        }

        @JvmStatic
        fun init() {
            setupUncaughtExceptionHandler {
                _isUncaughtException.value = true
            }
        }
    }
}