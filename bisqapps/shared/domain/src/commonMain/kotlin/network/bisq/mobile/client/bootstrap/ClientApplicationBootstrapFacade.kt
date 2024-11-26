package network.bisq.mobile.android.node.main.bootstrap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade

class ClientApplicationBootstrapFacade() :
    ApplicationBootstrapFacade() {

    override fun activate() {
        setState("Dummy state 1")
        setProgress(0f)

        // just dummy loading simulation, might be that there is no loading delay at the end...
        CoroutineScope(BackgroundDispatcher).launch {
            delay(50L)
            setState("Dummy state 2")
            setProgress(0.25f)

            delay(50L)
            setState("Dummy state 3")
            setProgress(0.5f)

            delay(50L)
            setState("Dummy state 4")
            setProgress(1f)
        }
    }

    override fun deactivate() {
    }
}