package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import kotlinx.coroutines.delay
import network.bisq.mobile.presentation.MainPresenter

open class SplashPresenter(
    mainPresenter: MainPresenter,
    private val navController: NavController
) : BasePresenter(mainPresenter), ISplashPresenter {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun startLoading(onProgressUpdate: (Float) -> Unit) {
        coroutineScope.launch {
            initializeNetwork{ progress ->
                onProgressUpdate(progress)
            }
            navigateToNextScreen()
        }
    }

    // TODO refactor into a service once networking is done
    protected open suspend fun initializeNetwork(updateProgress: (Float) -> Unit) {

        //1. Initialize Tor here
        //2. Connect to peers (for androidNode), to Bisq instance (for xClients)
        //3. Do any other app initialization
        for (i in 1..100) {
            updateProgress(i.toFloat() / 100)
            delay(25)
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
