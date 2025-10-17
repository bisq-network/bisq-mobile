package network.bisq.mobile.presentation.ui.uicases.banners

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

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

    val inventoryRequestInfo: StateFlow<String> = allDataReceived.map { isComplete ->
        if (isComplete) {
            "mobile.inventoryRequest.completed".i18n()
        } else {
            "mobile.inventoryRequest.requesting".i18n()
        }
    }.stateIn(
        presenterScope,
        SharingStarted.Lazily,
        "data.na".i18n()
    )
}
