package network.bisq.mobile.client.common.domain.service.push_notification

/**
 * Flavor seam for the relayed-push transport, declared once per distribution flavor.
 * Duplicated rather than expressed as expect/actual because those span Kotlin targets,
 * not Android product flavors.
 */
internal fun createPushNotificationTokenProvider(): PushNotificationTokenProvider = AndroidPushNotificationTokenProvider()
