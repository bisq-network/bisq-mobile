package network.bisq.mobile.android.node.presentation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.banners.BannerColumnPresenter

class NodeBannerColumnPresenter(
    mainPresenter: MainPresenter,
    private val networkServiceFacade: NetworkServiceFacade,
) : BannerColumnPresenter(mainPresenter, networkServiceFacade) {

    override fun observeConnectionState() {
        launchIO {
            combine(numConnections, isContentVisible) { numConnections, isContentVisible ->
                numConnections to isContentVisible
            }.collect { (numConnections, isContentVisible) ->
                _showConnectionsBanner.value = isContentVisible && numConnections < networkServiceFacade.minConnections
                _allConnectionsLost.value = numConnections <= 0
                _connectionState.value = if (numConnections <= 0)
                    "mobile.connectivity.banner.noConnections".i18n()
                else
                    "mobile.connectivity.banner.weakConnectivity".i18n(numConnections)
            }
        }
    }

    override fun observeInventoryRequestState() {
        launchIO {
            combine(allDataReceived, isContentVisible) { allDataReceived, isContentVisible ->
                allDataReceived to isContentVisible
            }.collect { (allDataReceived, isContentVisible) ->
                _showInventoryRequestBanner.value = isContentVisible

                _inventoryRequestState.value = if (allDataReceived)
                    "mobile.inventoryRequest.completed".i18n()
                else
                    "mobile.inventoryRequest.requesting".i18n()

                if (allDataReceived) {
                    // After data received we show for a while as feedback the state, then we hide the banner
                    launch {
                        delay(4000)
                        _showInventoryRequestBanner.value = false
                    }
                }
            }
        }
    }
}