package network.bisq.mobile.domain.data.model

import network.bisq.mobile.domain.getPlatform

/**
 * In general the models should remain closed, this is just an example from the time when we didn't have repositories and presenter
 */
open class Greeting: BaseModel() {
    protected val platform = getPlatform()
    protected open val greetText = "Hello, ${platform.name}!"

    fun greet(): String {
        return greetText
    }
}

interface GreetingFactory {
    fun createGreeting(): Greeting
}

class DefaultGreetingFactory : GreetingFactory {
    override fun createGreeting() = Greeting()
}