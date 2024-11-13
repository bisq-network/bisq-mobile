package network.bisq.mobile.domain.data.model

import network.bisq.mobile.domain.getPlatform

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