package network.bisq.mobile.i18n

/**
 * A deferred i18n string: a resource [key] plus optional [args], resolved at render time.
 *
 * Presenters emit [UiString] (keys, never resolved text); composables call [UiString.i18n] to
 * resolve it. This keeps translation/formatting a view concern and keeps presenters free of
 * resource lookups.
 */
data class UiString(
    val key: String,
    val args: List<Any> = emptyList(),
)

/** Convenience builder for a parameterized [UiString]. */
fun uiString(
    key: String,
    vararg args: Any,
): UiString = UiString(key, args.toList())

/** Resolves a [UiString] to localized text. An empty key renders as an empty string. */
fun UiString.i18n(): String =
    when {
        key.isEmpty() -> ""
        args.isEmpty() -> key.i18n()
        else -> key.i18n(*args.toTypedArray())
    }
