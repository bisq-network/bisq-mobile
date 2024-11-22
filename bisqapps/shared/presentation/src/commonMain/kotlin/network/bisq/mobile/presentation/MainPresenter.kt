package network.bisq.mobile.presentation

import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.*
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.presentation.ui.AppPresenter

/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter() : BasePresenter(null), AppPresenter {
    lateinit var navController: NavHostController
        private set

    override fun setNavController(controller: NavHostController) {
        navController = controller
    }

    private val log = Logger.withTag(this::class.simpleName ?: "MainPresenter")
    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible

    // passthrough example
//    private val _greetingText: StateFlow<String> = stateFlowFromRepository(
//        repositoryFlow = greetingRepository.data,
//        transform = { it?.greet() ?: "" },
//        initialValue = "Welcome!"
//    )
//    override val greetingText: StateFlow<String> = _greetingText

    init {
        log.i { "Shared Version: ${BuildConfig.SHARED_LIBS_VERSION}" }
        log.i { "iOS Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Node Version: ${BuildNodeConfig.APP_VERSION}" }
//        CoroutineScope(BackgroundDispatcher).launch {
//            greetingRepository.create(Greeting())
//        }
    }

    // Toggle action
    override fun toggleContentVisibility() {
        _isContentVisible.value = !_isContentVisible.value
    }


}