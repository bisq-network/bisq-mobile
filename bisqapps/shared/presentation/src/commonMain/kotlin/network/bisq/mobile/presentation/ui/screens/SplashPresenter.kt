package network.bisq.mobile.presentation.ui.screens

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.NetworkRepository
import network.bisq.mobile.presentation.ui.navigation.Routes

class SplashPresenter(
    private val navController: NavController,
    private val onProgressUpdate: (Float) -> Unit
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun startLoading() {
        coroutineScope.launch {
            NetworkRepository.initializeNetwork { progress ->
                onProgressUpdate(progress)
            }
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        navController.navigate(Routes.Onboarding.name) {
            popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }
}
