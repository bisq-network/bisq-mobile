package network.bisq.mobile.domain

interface LifeCycleAware {
    suspend fun activate() {}

    suspend fun deactivate() {}
}