package network.bisq.mobile.domain.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Exception thrown when an operation is cancelled by a signal flow (e.g., service deactivation).
 *
 * This is intentionally NOT a [kotlinx.coroutines.CancellationException] because it represents
 * an external signal cancelling a specific operation, not a coroutine being cancelled.
 * Using CancellationException for this purpose breaks on Kotlin/Native where it can escape
 * the coroutine framework and reach the unhandled exception hook, crashing the app.
 */
class OperationCancelledException(
    message: String = "Operation cancelled",
) : Exception(message)

private sealed class AwaitResult<T> {
    data class Value<T>(
        val value: T,
    ) : AwaitResult<T>()

    class Cancelled<T> : AwaitResult<T>()
}

/**
 * Awaits a value from a flow, but can be cancelled by a cancellation signal flow.
 *
 * @param valueFlow The flow to wait for a value from
 * @param cancelFlow The flow that signals cancellation
 * @param cancellationMessage The message to include in the OperationCancelledException
 */
suspend fun <T> awaitOrCancel(
    valueFlow: Flow<T>,
    cancelFlow: Flow<*>,
    cancellationMessage: String = "Operation cancelled",
): T {
    val result =
        merge(
            valueFlow.map { AwaitResult.Value(it) },
            cancelFlow.map { AwaitResult.Cancelled() },
        ).first()

    return when (result) {
        is AwaitResult.Value -> result.value
        is AwaitResult.Cancelled -> throw OperationCancelledException(cancellationMessage)
    }
}

/**
 * Awaits a value from a flow, but can return early with null by a cancellation signal flow.
 *
 * @param valueFlow The flow to wait for a value from
 * @param cancelFlow The flow that signals cancellation
 */
suspend fun <T> awaitOrNull(
    valueFlow: Flow<T>,
    cancelFlow: Flow<*>,
): T? {
    val result =
        merge(
            valueFlow.map { AwaitResult.Value(it) },
            cancelFlow.map { AwaitResult.Cancelled() },
        ).first()

    return when (result) {
        is AwaitResult.Value -> result.value
        is AwaitResult.Cancelled -> null
    }
}
