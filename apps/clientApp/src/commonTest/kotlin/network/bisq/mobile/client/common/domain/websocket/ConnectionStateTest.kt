package network.bisq.mobile.client.common.domain.websocket

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ConnectionStateTest {
    @Test
    fun `Disconnected without error has null error`() {
        val state = ConnectionState.Disconnected()
        assertNull(state.error)
    }

    @Test
    fun `Disconnected with error contains the error`() {
        val error = RuntimeException("Test error")
        val state = ConnectionState.Disconnected(error)
        assertEquals(error, state.error)
    }

    @Test
    fun `Connecting is a singleton object`() {
        val state1 = ConnectionState.Connecting
        val state2 = ConnectionState.Connecting
        assertSame(state1, state2)
    }

    @Test
    fun `Connected is a singleton object`() {
        val state1 = ConnectionState.Connected
        val state2 = ConnectionState.Connected
        assertSame(state1, state2)
    }

    @Test
    fun `Disconnected is a ConnectionState`() {
        val state: ConnectionState = ConnectionState.Disconnected()
        assertIs<ConnectionState.Disconnected>(state)
    }

    @Test
    fun `Connecting is a ConnectionState`() {
        val state: ConnectionState = ConnectionState.Connecting
        assertIs<ConnectionState.Connecting>(state)
    }

    @Test
    fun `Connected is a ConnectionState`() {
        val state: ConnectionState = ConnectionState.Connected
        assertIs<ConnectionState.Connected>(state)
    }

    @Test
    fun `Disconnected data class equality works`() {
        val error = RuntimeException("Test")
        val state1 = ConnectionState.Disconnected(error)
        val state2 = ConnectionState.Disconnected(error)
        assertEquals(state1, state2)
    }

    @Test
    fun `Disconnected copy works`() {
        val originalError = RuntimeException("Original")
        val newError = RuntimeException("New")
        val original = ConnectionState.Disconnected(originalError)
        val copied = original.copy(error = newError)
        assertEquals(newError, copied.error)
    }

    @Test
    fun `when expression covers all states`() {
        val states =
            listOf(
                ConnectionState.Disconnected(),
                ConnectionState.Connecting,
                ConnectionState.Connected,
            )

        states.forEach { state ->
            val result =
                when (state) {
                    is ConnectionState.Disconnected -> "disconnected"
                    is ConnectionState.Connecting -> "connecting"
                    is ConnectionState.Connected -> "connected"
                }
            when (state) {
                is ConnectionState.Disconnected -> assertEquals("disconnected", result)
                is ConnectionState.Connecting -> assertEquals("connecting", result)
                is ConnectionState.Connected -> assertEquals("connected", result)
            }
        }
    }
}
