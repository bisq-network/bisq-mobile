package network.bisq.mobile.presentation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.presentation.ui.AppPresenter

/**
 * Main Presenter as an example of implementation for now.
 */
class MainPresenter(private val greetingRepository: GreetingRepository) : BasePresenter(), AppPresenter {
    private val log = Logger.withTag("MainPresenter")
    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible

    private val _greetingText = MutableStateFlow("Welcome!")
    override val greetingText: StateFlow<String> = _greetingText

    fun refresh() {
        _greetingText.value = greetingRepository.getValue()
    }

    // Toggle action
    override fun toggleContentVisibility() {
        _isContentVisible.value = !_isContentVisible.value
    }

    override fun onResume() {
        super.onResume()
        log.i { "Shared Version: ${BuildConfig.SHARED_LIBS_VERSION}" }
        log.i { "iOS Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Node Version: ${BuildNodeConfig.APP_VERSION}" }
        refresh()
    }
}