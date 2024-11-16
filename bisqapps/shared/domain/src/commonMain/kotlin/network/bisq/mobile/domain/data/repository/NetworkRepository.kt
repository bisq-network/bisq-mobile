    package network.bisq.mobile.domain.data.repository
    
    import kotlinx.coroutines.delay
    import network.bisq.mobile.domain.data.model.NetworkModel

    open class NetworkRepository<T : NetworkModel> : SingleObjectRepository<T>() {

        suspend fun initializeNetwork(updateProgress: (Float) -> Unit) {

            //1. Initialize Tor here
            //2. Connect to peers (for androidNode), to Bisq instance (for xClients)
            //3. Do any other app initialization
            for (i in 1..100) {
                updateProgress(i.toFloat() / 100)
                delay(25)
            }
        }
    }