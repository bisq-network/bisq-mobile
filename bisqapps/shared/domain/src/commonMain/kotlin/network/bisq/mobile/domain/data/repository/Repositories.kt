package network.bisq.mobile.domain.data.repository

import network.bisq.mobile.domain.data.model.Greeting

// this way of definingsupports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class GreetingRepository<T: Greeting>: SingleObjectRepository<T>()
