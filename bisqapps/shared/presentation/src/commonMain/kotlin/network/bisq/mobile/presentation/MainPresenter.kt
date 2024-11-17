package network.bisq.mobile.presentation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.presentation.ui.AppPresenter

/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter(private val greetingRepository: GreetingRepository<Greeting>) : BasePresenter(null), AppPresenter {
    private val log = Logger.withTag("MainPresenter")
    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible

    // TODO remove we don't need all the gretting example stuff anymore - but this code to passthrough should be a helper for presentation
    private val _greetingText: StateFlow<String> = greetingRepository.data
        .map { it?.greet() ?: "" } // Transform Greeting to String, we don't want the null
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main), // Use an appropriate CoroutineScope for lifecycle control
            started = SharingStarted.Lazily,
            initialValue = "Welcome!"
        )

    override val greetingText: StateFlow<String> = _greetingText

    init {
        CoroutineScope(Dispatchers.IO).launch {
            greetingRepository.create(Greeting())
        }
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
    }

    override fun onPause() {
        super.onPause()
    }
}