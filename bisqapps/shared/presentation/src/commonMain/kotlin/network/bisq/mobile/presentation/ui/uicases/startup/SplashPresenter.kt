package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.NetworkRepository
import network.bisq.mobile.domain.data.model.NetworkModel
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import kotlinx.coroutines.delay

open class SplashPresenter(
    private val navController: NavController,
    private val networkRepository: NetworkRepository<NetworkModel>
) : BasePresenter(), ISplashPresenter {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun startLoading(onProgressUpdate: (Float) -> Unit) {
        coroutineScope.launch {
            networkRepository?.initializeNetwork{ progress ->
                onProgressUpdate(progress)
            }
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        // TODO: Conditional nav
        // If firstTimeApp launch, goto Onboarding[clientMode] (androidNode / xClient)
        // If not, goto TabContainerScreen
        navController.navigate(Routes.Onboarding.name) {
            popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

}
