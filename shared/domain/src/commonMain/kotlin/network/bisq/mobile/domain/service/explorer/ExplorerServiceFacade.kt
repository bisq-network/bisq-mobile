package network.bisq.mobile.domain.service.explorer

import network.bisq.mobile.domain.LifeCycleAware

interface ExplorerServiceFacade : LifeCycleAware {
    suspend fun getSelectedBlockExplorer(): Result<String>
    suspend fun requestTx(txId: String, address: String): ExplorerResult
}