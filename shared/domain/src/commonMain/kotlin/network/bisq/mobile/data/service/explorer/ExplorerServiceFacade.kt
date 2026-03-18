package network.bisq.mobile.data.service.explorer

import network.bisq.mobile.data.service.LifeCycleAware

interface ExplorerServiceFacade : LifeCycleAware {
    suspend fun getSelectedBlockExplorer(): Result<String>

    suspend fun requestTx(
        txId: String,
        address: String,
    ): ExplorerResult
}
