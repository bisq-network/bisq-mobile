package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class SplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    val state: StateFlow<String> = applicationBootstrapFacade.state
    val progress: StateFlow<Float> = applicationBootstrapFacade.progress

    private var job: Job? = null
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)

    override fun onViewAttached() {
        job = coroutineScope.launch {
            progress.collect { value ->
                when {
                    value == 1.0f -> navigateToNextScreen()
                }
            }
        }
    }

    override fun onViewUnattaching() {
        cancelJob()
    }

    private fun navigateToNextScreen() {
        CoroutineScope(Dispatchers.Main).launch {
            if (userProfileService.hasUserProfile()) {
                rootNavigator.navigate(Routes.TabContainer.name) {
                    popUpTo(Routes.Splash.name) { inclusive = true }
                }
            } else {
                rootNavigator.navigate(Routes.CreateProfile.name) {
                    popUpTo(Routes.Splash.name) { inclusive = true }
                }
            }
        }
    }

    private fun cancelJob() {
        try {
            job?.cancel()
            job = null
        } catch (e: CancellationException) {
            log.e("Job cancel failed", e)
        }
    }
}
