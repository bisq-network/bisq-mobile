package network.bisq.mobile.domain

import network.bisq.mobile.domain.data.model.Greeting
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidGreetingTest {

    @Test
    fun testExample() {
        assertTrue(Greeting().greet().contains("Android"), "Check iOS is mentioned")
    }
}