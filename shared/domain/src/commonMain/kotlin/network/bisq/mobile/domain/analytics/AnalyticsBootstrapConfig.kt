package network.bisq.mobile.domain.analytics

/**
 * Per-app configuration values handed to [AnalyticsService.init] at bootstrap.
 *
 * Each app (`clientApp` Android, `clientApp` iOS, `nodeApp` Android) provides
 * its own instance via DI, sourcing values from the app's `BuildConfig`. This
 * keeps the lifecycle bootstrap code platform-agnostic — it just calls
 * `init(config.dsn, config.environment, config.release)`.
 *
 * An empty [dsn] is the convention for "analytics not configured" and
 * [AnalyticsService.init] implementations refuse to dial in that case.
 */
data class AnalyticsBootstrapConfig(
    val dsn: String,
    val environment: String,
    val release: String,
)
