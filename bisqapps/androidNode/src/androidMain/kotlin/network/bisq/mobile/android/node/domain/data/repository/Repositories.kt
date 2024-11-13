package network.bisq.mobile.android.node.domain.data.repository

import network.bisq.mobile.android.node.AndroidNodeGreeting
import network.bisq.mobile.domain.data.repository.GreetingRepository

// this way of definingsupports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
class NodeGreetingRepository: GreetingRepository<AndroidNodeGreeting>()