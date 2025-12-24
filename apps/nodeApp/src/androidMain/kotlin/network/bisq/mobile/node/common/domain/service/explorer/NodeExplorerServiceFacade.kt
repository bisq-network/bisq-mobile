package network.bisq.mobile.node.common.domain.service.explorer

import bisq.bonded_roles.explorer.ExplorerService
import bisq.bonded_roles.explorer.dto.Tx
import kotlinx.coroutines.future.await
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerResult
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.utils.ExceptionUtils.getRootCause
import network.bisq.mobile.domain.utils.ExceptionUtils.getRootCauseMessage
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeExplorerServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    ExplorerServiceFacade {
    // Dependencies
    private val explorerService: ExplorerService by lazy { applicationService.bondedRolesService.get().explorerService }
    private val cachedExplorerResults: MutableMap<String, ExplorerResult> = HashMap()

    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getSelectedBlockExplorer(): Result<String> {
        log.i { "Selected block explorer " + explorerService.selectedProvider.get().baseUrl }
        return Result.success(explorerService.selectedProvider.get().baseUrl)
    }

    override suspend fun requestTx(
        txId: String,
        address: String,
    ): ExplorerResult {
        log.i { "requestTx $txId $address" }
        try {
            if (cachedExplorerResults.containsKey(txId)) {
                return cachedExplorerResults[txId]!!
            }
            val tx = explorerService.requestTx(txId).await()
            val isConfirmed = tx.status.isConfirmed
            val outputValues = filterOutputsByAddress(tx, address)
            val explorerResult = ExplorerResult(isConfirmed, outputValues)
            if (isConfirmed) {
                cachedExplorerResults[txId] = explorerResult
            }
            log.i { "explorerResult $explorerResult" }
            return explorerResult
        } catch (throwable: Throwable) {
            log.e { "Request transaction from ${explorerService.selectedProvider.get().baseUrl} failed with $throwable" }
            val exceptionName = throwable.getRootCause()::class.simpleName ?: "Unknown exception"
            val errorMessage = throwable.getRootCauseMessage()
            return ExplorerResult(exceptionName = exceptionName, errorMessage = errorMessage)
        }
    }

    private fun filterOutputsByAddress(
        tx: Tx,
        address: String,
    ): List<Long> = tx.outputs.filter { it.address == address }.map { it.value }
}
