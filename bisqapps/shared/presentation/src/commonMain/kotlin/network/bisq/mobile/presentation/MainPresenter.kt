package network.bisq.mobile.presentation

import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.presentation.ui.AppPresenter


/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter(private val applicationBootstrapFacade: ApplicationBootstrapFacade) : BasePresenter(null), AppPresenter {
    lateinit var navController: NavHostController
    private set

    override fun setNavController(controller: NavHostController) {
        navController = controller
    }

    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible

    private var applicationServiceInited = false

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

    override fun onViewAttached() {
        super.onViewAttached()

        if (!applicationServiceInited) {
            initializeServices()
            applicationServiceInited = true
        }
    }

    protected open fun initializeServices() {
        applicationBootstrapFacade.initialize()
    }

    // Toggle action
    override fun toggleContentVisibility() {
        _isContentVisible.value = !_isContentVisible.value
    }


}