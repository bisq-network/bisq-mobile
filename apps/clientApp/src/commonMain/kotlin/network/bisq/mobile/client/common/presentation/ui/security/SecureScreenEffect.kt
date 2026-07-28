package network.bisq.mobile.client.common.presentation.ui.security

import androidx.compose.runtime.Composable

/**
 * Marks the enclosing screen as security-sensitive. The strength of the protection differs
 * per platform: Android blocks screenshots and screen recording and blanks the app-switcher /
 * Recents thumbnail; iOS only obscures the app-switcher snapshot and is not equivalent to
 * Android's `FLAG_SECURE` (it does not block screenshots or screen recording).
 *
 * Place this at the top of any Composable that renders secrets (e.g. the trusted-node
 * pairing screen, whose pairing code embeds the Tor client-auth secret, TLS fingerprint
 * and node URLs). The protection is scoped to the screen: it is applied while the
 * Composable is in the composition and removed when it leaves, so the rest of the app
 * keeps normal behaviour.
 *
 * - Android: sets `WindowManager.LayoutParams.FLAG_SECURE` on the host window — screenshots
 *   and screen recording are blocked and the Recents thumbnail is blanked.
 * - iOS: covers the window with a blur only during resign-active / background transitions, so
 *   the snapshot captured for the app switcher does not expose the content. Screenshots and
 *   screen recording while the app is active are not prevented.
 */
@Composable
expect fun SecureScreenEffect()
