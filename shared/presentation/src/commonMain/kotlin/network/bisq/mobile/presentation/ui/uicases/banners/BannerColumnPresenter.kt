package network.bisq.mobile.presentation.ui.uicases.banners

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class BannerColumnPresenter(
    private val mainPresenter: MainPresenter,
    private val networkServiceFacade: NetworkServiceFacade,
) : BasePresenter(mainPresenter) {
    protected val _showConnectionsBanner = MutableStateFlow(false)
    val showConnectionsBanner: StateFlow<Boolean> get() = _showConnectionsBanner.asStateFlow()

    protected val _showInventoryRequestBanner = MutableStateFlow(false)
    val showInventoryRequestBanner: StateFlow<Boolean> get() = _showInventoryRequestBanner.asStateFlow()

    protected val _inventoryRequestState = MutableStateFlow("")
    val inventoryRequestState: StateFlow<String> get() = _inventoryRequestState.asStateFlow()

    protected val _allConnectionsLost = MutableStateFlow(false)
    val allConnectionsLost: StateFlow<Boolean> get() = _allConnectionsLost.asStateFlow()

    protected val _connectionState = MutableStateFlow("")
    val connectionState: StateFlow<String> get() = _connectionState.asStateFlow()

    val allDataReceived: StateFlow<Boolean> get() = networkServiceFacade.allDataReceived
    val numConnections: StateFlow<Int> get() = networkServiceFacade.numConnections

    val isContentVisible: StateFlow<Boolean> get() = mainPresenter.isMainContentVisible

    override fun onViewAttached() {
        super.onViewAttached()

        observeConnectionState()

        observeInventoryRequestState()
    }

    protected open fun observeConnectionState() {
        // For node implementation is in NodeBannerColumnPresenter
        // For client we don't have yet the backend API. But not sure if that is really needed for client
    }

    protected open fun observeInventoryRequestState() {
        // For node implementation is in NodeBannerColumnPresenter
        // For client we don't have yet the backend API. But not sure if that is really needed for client
    }
}
