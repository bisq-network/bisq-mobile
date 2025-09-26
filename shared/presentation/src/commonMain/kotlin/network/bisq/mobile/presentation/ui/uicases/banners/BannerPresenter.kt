package network.bisq.mobile.presentation.ui.uicases.banners

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class BannerPresenter(
    private val mainPresenter: MainPresenter,
    private val networkServiceFacade: NetworkServiceFacade,
) : BasePresenter(mainPresenter) {

    val allDataReceived: StateFlow<Boolean> get() = networkServiceFacade.allDataReceived
    val numConnections: StateFlow<Int> get() = networkServiceFacade.numConnections
    val isMainContentVisible: StateFlow<Boolean> get() = mainPresenter.isMainContentVisible

    val inventoryRequestInfo: StateFlow<String> =
        allDataReceived.map {
            if (it) "mobile.inventoryRequest.completed".i18n() else "mobile.inventoryRequest.requesting".i18n()
        }.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            "data.na".i18n()
        )
}
