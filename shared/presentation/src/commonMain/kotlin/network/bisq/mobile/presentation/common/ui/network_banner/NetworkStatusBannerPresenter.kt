package network.bisq.mobile.presentation.common.ui.network_banner

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Presenter for NetworkStatusBanner component.
 *
 * Exposes network connection state and data synchronization progress
 * for displaying banner visibility and content to the user.
 */
open class NetworkStatusBannerPresenter(
    mainPresenter: MainPresenter,
    networkServiceFacade: NetworkServiceFacade,
) : BasePresenter(mainPresenter) {
    val allDataReceived: StateFlow<Boolean> = networkServiceFacade.allDataReceived
    val numConnections: StateFlow<Int> = networkServiceFacade.numConnections
    val isMainContentVisible: StateFlow<Boolean> = mainPresenter.isMainContentVisible

    val inventoryRequestInfo: StateFlow<UiString> =
        allDataReceived
            .map { isComplete ->
                if (isComplete) {
                    UiString("mobile.inventoryRequest.completed")
                } else {
                    UiString("mobile.inventoryRequest.requesting")
                }
            }.stateIn(
                presenterScope,
                SharingStarted.Lazily,
                UiString("data.na"),
            )
}
