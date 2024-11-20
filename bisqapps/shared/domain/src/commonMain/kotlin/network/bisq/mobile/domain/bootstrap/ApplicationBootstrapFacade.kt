package network.bisq.mobile.domain.data.repository.main.bootstrap

interface ApplicationBootstrapFacade {
    fun initialize()
    val model: ApplicationBootstrapModel
}