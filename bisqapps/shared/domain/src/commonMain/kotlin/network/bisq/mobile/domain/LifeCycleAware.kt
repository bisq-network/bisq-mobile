package network.bisq.mobile.domain

interface LifeCycleAware {
    fun initialize()

    fun resume()

    fun dispose()
}