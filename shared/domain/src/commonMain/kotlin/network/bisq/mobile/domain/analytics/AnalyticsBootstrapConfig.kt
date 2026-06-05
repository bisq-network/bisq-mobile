package network.bisq.mobile.domain.analytics

/**
 * Per-app configuration values handed to [AnalyticsService.init] at bootstrap.
 *
 * Each app (`clientApp` Android, `clientApp` iOS, `nodeApp` Android) provides
 * its own instance via DI, sourcing values from the app's `BuildConfig`. This
 * keeps the lifecycle bootstrap code platform-agnostic — it just calls
 * `init(config.dsn, config.environment, config.release, config.isDebug)`.
 *
 * An empty [dsn] is the convention for "analytics not configured" and
 * [AnalyticsService.init] implementations refuse to dial in that case.
 *
 * [isDebug] gates the underlying SDK's verbose internal logging (envelope
 * POSTs, transport failures, etc.). When `true` the SDK prints diagnostic
 * chatter to logcat at INFO level — useful for verifying ingestion locally
 * during dev. When `false` only ERROR-level SDK problems are logged, keeping
 * release builds quiet even when a user opts in. Sourced from each app's
 * `BuildConfig.IS_DEBUG` (or `BuildNodeConfig.IS_DEBUG` for nodeApp).
 */
data class AnalyticsBootstrapConfig(
    val dsn: String,
    val environment: String,
    val release: String,
    val isDebug: Boolean,
)
