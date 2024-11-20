package network.bisq.mobile.android.node.main.bootstrap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade

class ClientApplicationBootstrapFacade() :
    ApplicationBootstrapFacade() {
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)

    override fun initialize() {
        setState("Dummy state 1")
        setProgress(0f)

        // just dummy loading simulation, might be that there is no loading delay at the end...
        coroutineScope.launch {
            delay(500L)
            setState("Dummy state 2")
            setProgress(0.25f)

            delay(500L)
            setState("Dummy state 3")
            setProgress(0.5f)

            delay(500L)
            setState("Dummy state 4")
            setProgress(1f)
        }
    }
}