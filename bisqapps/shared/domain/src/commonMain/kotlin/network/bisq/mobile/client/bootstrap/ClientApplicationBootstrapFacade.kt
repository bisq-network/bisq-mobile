package network.bisq.mobile.android.node.main.bootstrap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapModel

class ClientApplicationBootstrapFacade(
    override val model: ApplicationBootstrapModel
) :
    ApplicationBootstrapFacade {
    private val coroutineScope =  CoroutineScope(BackgroundDispatcher)

    init {
        val model = model as ClientApplicationBootstrapModel
        model.setState( "Dummy state 1")
        model.setProgress(0f)

        // just dummy loading simulation, might be that there is no loading delay at the end...
        coroutineScope.launch {
            delay(500L)
            model.setState( "Dummy state 2")
            model.setProgress(0.25f)

            delay(500L)
            model.setState( "Dummy state 3")
            model.setProgress(0.5f)

            delay(500L)
            model.setState( "Dummy state 4")
            model.setProgress(1f)
        }
    }
}