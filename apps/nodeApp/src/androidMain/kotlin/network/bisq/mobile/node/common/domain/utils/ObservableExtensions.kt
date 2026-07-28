package network.bisq.mobile.node.common.domain.utils

import bisq.common.observable.Pin
import bisq.common.observable.ReadOnlyObservable
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Bridges a bisq2 [ReadOnlyObservable] into a [MutableStateFlow].
 *
 * Bisq2 observables invoke the observer synchronously at subscription with the CURRENT
 * value, which may be null before data has been loaded from disk. Use [bindNonNullTo]
 * (or handle null inside [map]) when the target flow must not see that null.
 */
fun <T> ReadOnlyObservable<T>.bindTo(target: MutableStateFlow<T>): Pin =
    addObserver { value ->
        target.value = value
    }

fun <B, D> ReadOnlyObservable<B>.bindTo(
    target: MutableStateFlow<D>,
    map: (B) -> D,
): Pin =
    addObserver { value ->
        target.value = map(value)
    }

/** Like [bindTo] but null emissions are ignored instead of overwriting the target. */
fun <T : Any> ReadOnlyObservable<T>.bindNonNullTo(target: MutableStateFlow<in T>): Pin =
    addObserver { value ->
        if (value != null) {
            target.value = value
        }
    }

fun <B : Any, D> ReadOnlyObservable<B>.bindNonNullTo(
    target: MutableStateFlow<D>,
    map: (B) -> D,
): Pin =
    addObserver { value ->
        if (value != null) {
            target.value = map(value)
        }
    }

/**
 * Like [bindTo] but pushes each value into [set] instead of a [MutableStateFlow].
 *
 * Use this to bridge into a model that only exposes read-only state plus explicit
 * setters (e.g. `BisqEasyTradeModel`), keeping the backing flow encapsulated.
 */
fun <T> ReadOnlyObservable<T>.bindTo(set: (T) -> Unit): Pin =
    addObserver { value ->
        set(value)
    }

/** Like [bindNonNullTo] but pushes each non-null value into [set]. */
fun <T : Any> ReadOnlyObservable<T>.bindNonNullTo(set: (T) -> Unit): Pin =
    addObserver { value ->
        if (value != null) {
            set(value)
        }
    }

/** Like [bindNonNullTo] with map but pushes each transformed non-null value into [set]. */
fun <B : Any, D> ReadOnlyObservable<B>.bindNonNullTo(
    map: (B) -> D,
    set: (D) -> Unit,
): Pin =
    addObserver { value ->
        if (value != null) {
            set(map(value))
        }
    }
