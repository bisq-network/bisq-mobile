package network.bisq.mobile.client.common.domain.service.push_notification

/**
 * Flavor seam for the relayed-push transport. See the `google` flavor's copy for why this is
 * duplicated rather than expressed as expect/actual.
 */
internal fun createPushNotificationTokenProvider(): PushNotificationTokenProvider = UnsupportedPushNotificationTokenProvider()

/**
 * Stands in for a transport this flavor does not have. FCM is the only relayed transport wired
 * to the trusted node, and it cannot ship on F-Droid, whose inclusion policy rejects proprietary
 * dependencies outright. Background delivery here comes from the local foreground service holding
 * the trusted-node WebSocket open instead.
 *
 * Reporting [isSupported] as false travels up through
 * `PushNotificationServiceFacade.isRelayedPushSupported` into `SettingsUiState`, where the
 * settings section still renders but with the switch disabled and an explanation in place of the
 * usual tail text. So these methods exist to keep the contract total rather than to be called.
 * They still fail rather than fake success: a silent `Result.success` would register the device
 * with the trusted node under a token nothing can deliver to, and the user would believe push is
 * working.
 */
internal class UnsupportedPushNotificationTokenProvider : PushNotificationTokenProvider {
    override val isSupported: Boolean = false

    override suspend fun requestPermission(): Boolean = false

    override suspend fun requestDeviceToken(): Result<String> = Result.failure(UnsupportedOperationException("This build ships no relayed push transport"))
}
