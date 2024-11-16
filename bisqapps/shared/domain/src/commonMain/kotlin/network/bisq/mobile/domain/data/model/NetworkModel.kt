package network.bisq.mobile.domain.data.model

import kotlinx.coroutines.delay

open class NetworkModel: BaseModel() {
    var progress: Float = 0.0f
}

interface NetworkModelFactory {
    fun createNetworkModel(): NetworkModel
}

class DefaultNetworkModelFactory : NetworkModelFactory {
    override fun createNetworkModel() = NetworkModel()
}