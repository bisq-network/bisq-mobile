package network.bisq.mobile.android.node

import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.model.GreetingFactory

class AndroidNodeGreeting : Greeting() {
    override val greetText = "Hello Node, ${platform.name}!"
}
class AndroidNodeGreetingFactory : GreetingFactory {
    override fun createGreeting() = AndroidNodeGreeting()
}