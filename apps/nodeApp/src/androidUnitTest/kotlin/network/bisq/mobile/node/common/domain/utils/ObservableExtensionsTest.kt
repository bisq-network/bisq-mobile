package network.bisq.mobile.node.common.domain.utils

import bisq.common.observable.Observable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObservableExtensionsTest {
    @Test
    fun `bindTo pushes current value at subscription and follows updates`() {
        val observable = Observable("a")
        val target = MutableStateFlow("initial")

        val pin = observable.bindTo(target)

        assertEquals("a", target.value)
        observable.set("b")
        assertEquals("b", target.value)

        pin.unbind()
        observable.set("c")
        assertEquals("b", target.value, "unbound pin must stop propagation")
    }

    @Test
    fun `bindTo with map transforms values`() {
        val observable = Observable(1)
        val target = MutableStateFlow("")

        val pin = observable.bindTo(target) { "v$it" }

        assertEquals("v1", target.value)
        observable.set(2)
        assertEquals("v2", target.value)

        pin.unbind()
        observable.set(3)
        assertEquals("v2", target.value, "unbound pin must stop propagation")
    }

    @Test
    fun `bindNonNullTo ignores null current value at subscription`() {
        // bisq2 observables fire synchronously at subscription with the CURRENT
        // value, which is null before first set — bindNonNullTo must not
        // overwrite the target with it.
        val observable = Observable<String>()
        val target = MutableStateFlow("initial")

        observable.bindNonNullTo(target)

        assertEquals("initial", target.value)
        observable.set("a")
        assertEquals("a", target.value)
    }

    @Test
    fun `bindNonNullTo with map ignores nulls and transforms values`() {
        val observable = Observable<Int>()
        val target = MutableStateFlow<String?>(null)

        val pin = observable.bindNonNullTo(target) { "v$it" }

        assertNull(target.value)
        observable.set(7)
        assertEquals("v7", target.value)

        pin.unbind()
        observable.set(8)
        assertEquals("v7", target.value, "unbound pin must stop propagation")
    }
}
