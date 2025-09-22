package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class SupportPresenter(mainPresenter: MainPresenter) : BasePresenter(mainPresenter) {
    fun onOpenWebUrl(url: String) {
        navigateToUrl(url)
    }

    fun onRestartApp() {
        restartApp()
    }

    fun onTerminateApp() {
        terminateApp()
    }
}