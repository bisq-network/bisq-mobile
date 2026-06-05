package network.bisq.mobile.domain.analytics

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel

/**
 * Thin abstraction over the Sentry-KMP top-level [Sentry] object. The only
 * reason this exists is unit-testability: the production code path goes through
 * the global `Sentry` singleton which cannot be reset between tests, but our
 * own [SentryAnalyticsService] should be exercisable in isolation with a fake
 * collaborator. Production code wires [DefaultSentryClient]; tests inject a
 * fake.
 *
 * Methods are 1:1 with the [Sentry] API surface we actually use — kept narrow
 * on purpose so the privacy review surface is small.
 */
internal interface SentryClient {
    /**
     * Initialise the underlying SDK with the privacy posture required by
     * bisq-network/bisq-mobile#525:
     *  - `sendDefaultPii = false` (no IP, no user, no device id auto-attached).
     *  - `beforeSend` runs the [AnalyticsRedactor] over message + exception
     *    text as a defence-in-depth layer for whatever made it past the
     *    sealed-event API.
     *
     * Implementations MUST be safe to call only once per process — Sentry-KMP
     * keeps internal init state and re-init is undefined.
     *
     * [isDebug] gates the SDK's own verbose internal logging — see the
     * matching field on [AnalyticsBootstrapConfig] for the rationale.
     */
    fun init(
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
    )

    /**
     * Emit a discrete event. Backs [AnalyticsService.track]. Implementations
     * MUST drop the call silently if the SDK isn't initialised.
     */
    fun captureMessage(message: String)

    /**
     * Capture an exception. Backs [AnalyticsService.captureException]. The
     * SDK auto-installs an `UncaughtExceptionHandler` once initialised, so
     * this is for *handled* exceptions you explicitly want to ship.
     */
    fun captureException(throwable: Throwable)
}

/**
 * Production implementation. The only place in the codebase that touches
 * Sentry-KMP types directly — keeps the SDK touch surface to one file so the
 * privacy review (and any future SDK bump) has a single grep target.
 */
internal object DefaultSentryClient : SentryClient {
    override fun init(
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
    ) {
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = environment
            options.release = release
            // Verification aid in debug builds only: surface the SDK's internal
            // log lines (envelope POSTs, transport failures, etc.) so we can
            // see WHY an event isn't arriving when GlitchTip's UI is empty
            // despite a successful `track()` call. SentryLevel.INFO is the
            // loudest level worth running — DEBUG floods logcat with every
            // breadcrumb. In release builds we drop to ERROR-only so opted-in
            // users don't see Sentry chatter in their logs. Sourced from the
            // app's BuildConfig.IS_DEBUG via [AnalyticsBootstrapConfig] — see
            // [SentryClient.init] kdoc.
            options.debug = isDebug
            options.diagnosticLevel = if (isDebug) SentryLevel.INFO else SentryLevel.ERROR
            // The two layers that make this analytics integration shippable per
            // the privacy agreement on issue #525:
            options.sendDefaultPii = false
            options.beforeSend = { event ->
                // Defence-in-depth: scrub message + exception text through the
                // PII redactor before the SDK serialises the event. The sealed
                // AnalyticsEvent API is the primary guarantee; this catches
                // whatever runtime-constructed strings (stack frame messages,
                // JVM-formatted FileNotFoundException paths, etc.) sneak in.
                event.message?.let { msg ->
                    msg.message = msg.message?.let { redactor.redact(it) }
                    msg.formatted = msg.formatted?.let { redactor.redact(it) }
                }
                // SentryException is a data class with val fields; the list
                // itself is mutable so we replace each entry in place with a
                // redacted copy.
                for (i in event.exceptions.indices) {
                    val ex = event.exceptions[i]
                    val redactedValue = ex.value?.let { redactor.redact(it) }
                    if (redactedValue != ex.value) {
                        event.exceptions[i] = ex.copy(value = redactedValue)
                    }
                }
                event
            }
        }
    }

    override fun captureMessage(message: String) {
        Sentry.captureMessage(message)
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}
