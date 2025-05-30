package network.bisq.mobile.client.service.explorer

import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerResult
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade

class ClientExplorerServiceFacade(private val apiGateway: ExplorerApiGateway) : ServiceFacade(), ExplorerServiceFacade {
    private val cachedExplorerResults: MutableMap<String, ExplorerResult> = HashMap()

    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getSelectedBlockExplorer(): Result<String> {
        val result = apiGateway.getSelectedBlockExplorer()
        return if (result.isSuccess) {
            Result.success(result.getOrThrow()["provider"]!!)
        } else {
            Result.failure(result.exceptionOrNull() ?: IllegalStateException("No Exception is set in result failure"))
        }
    }

    override suspend fun requestTx(txId: String, address: String): ExplorerResult {
        if (cachedExplorerResults.containsKey(txId)) {
            return cachedExplorerResults[txId]!!
        }

        val result = apiGateway.requestTx(txId)
        if (result.isSuccess) {
            val tx: ExplorerTxDto = result.getOrThrow()
            val isConfirmed = tx.isConfirmed
            val outputValues = filterOutputsByAddress(tx, address)
            val explorerResult = ExplorerResult(isConfirmed, outputValues)
            if (isConfirmed) {
                cachedExplorerResults[txId] = explorerResult
            }
            log.i { "explorerResult $explorerResult" }
            return explorerResult
        } else {
            return ExplorerResult(errorMessage = result.exceptionOrNull()?.message ?: "No Exception is set in result failure")
        }
    }

    private fun filterOutputsByAddress(tx: ExplorerTxDto, address: String): List<Long> =
        tx.outputs.filter { it.address == address }.map { it.value }
}