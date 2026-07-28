package network.bisq.mobile.client.common.presentation.ui.security

import androidx.compose.runtime.Composable

/**
 * Marks the enclosing screen as security-sensitive so the OS does not leak its
 * contents through screenshots, screen recordings, or the app-switcher / Recents
 * thumbnail.
 *
 * Place this at the top of any Composable that renders secrets (e.g. the trusted-node
 * pairing screen, whose pairing code embeds the Tor client-auth secret, TLS fingerprint
 * and node URLs). The protection is scoped to the screen: it is applied while the
 * Composable is in the composition and removed when it leaves, so the rest of the app
 * keeps normal screenshot behaviour.
 *
 * - Android: sets `WindowManager.LayoutParams.FLAG_SECURE` on the host window.
 * - iOS: covers the window with a blur while the app is inactive/backgrounded so the
 *   snapshot captured for the app switcher does not expose the content.
 */
@Composable
expect fun SecureScreenEffect()
