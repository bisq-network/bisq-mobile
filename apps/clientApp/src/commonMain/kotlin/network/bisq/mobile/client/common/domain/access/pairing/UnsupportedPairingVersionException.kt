package network.bisq.mobile.client.common.domain.access.pairing

/**
 * Thrown when a pairing code or pairing QR payload declares a format version this app
 * version does not understand. Kept as a distinct type so the UI can tell the user to
 * update the app instead of showing the generic "invalid format" error.
 */
class UnsupportedPairingVersionException(
    message: String,
) : IllegalArgumentException(message)
