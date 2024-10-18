package network.bisq.mobile.android.node

import bisq.common.application.ApplicationVersion
import network.bisq.mobile.Greeting
import network.bisq.mobile.GreetingFactory

class AndroidNodeGreeting : Greeting() {
    override fun greet(): String {
        return "Hello Node, ${platform.name}!\n    Bisq Core: ${ApplicationVersion.getVersion().versionAsString}"
    }
}
class AndroidNodeGreetingFactory : GreetingFactory {
    override fun createGreeting() = AndroidNodeGreeting()
}