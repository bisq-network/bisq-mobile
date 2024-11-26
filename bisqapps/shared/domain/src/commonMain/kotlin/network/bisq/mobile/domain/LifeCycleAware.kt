package network.bisq.mobile.domain

interface LifeCycleAware {
    fun activate()

    fun deactivate()
}