package network.bisq.mobile.node.network.presentation.my_node

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class NetworkMyNodePresenter(
    private val networkServiceFacade: NodeNetworkServiceFacade,
    private val kmpTorService: KmpTorService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(NetworkMyNodeUiState())
    val uiState: StateFlow<NetworkMyNodeUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch {
            combine(
                networkServiceFacade.myNodeInfo,
                kmpTorService.state,
            ) { info, torState ->
                NetworkMyNodeUiState(
                    onionAddress = info.onionAddress,
                    keyId = info.keyId,
                    appVersion = BuildNodeConfig.APP_VERSION,
                    isTorRunning = torState is KmpTorService.TorState.Started,
                )
            }.collect { _uiState.value = it }
        }
    }
}
