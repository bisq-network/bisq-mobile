package network.bisq.mobile.data.service

interface LifeCycleAware {
    suspend fun activate() {}

    suspend fun deactivate() {}
}
