package network.bisq.mobile.client.common.presentation.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.interop.LocalUIViewController
import platform.Foundation.NSNotificationCenter
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIVisualEffectView

/**
 * iOS has no `FLAG_SECURE` equivalent, so instead of trying to block screenshots we protect
 * the one leak that happens without any user action: the snapshot iOS takes for the app
 * switcher when the app resigns active. While this Composable is shown we cover the window
 * with a blur on `willResignActive` and remove it on `didBecomeActive`, so the app-switcher
 * thumbnail does not expose the on-screen secrets. Observers are removed on dispose.
 */
@Composable
actual fun SecureScreenEffect() {
    val viewController = LocalUIViewController.current

    DisposableEffect(viewController) {
        val notificationCenter = NSNotificationCenter.defaultCenter
        var coverView: UIVisualEffectView? = null

        fun addCover() {
            if (coverView != null) return
            val window = viewController.view.window ?: return
            val blur = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular)
            val effectView = UIVisualEffectView(effect = blur)
            effectView.translatesAutoresizingMaskIntoConstraints = false
            window.addSubview(effectView)
            NSLayoutConstraint.activateConstraints(
                listOf(
                    effectView.topAnchor.constraintEqualToAnchor(window.topAnchor),
                    effectView.bottomAnchor.constraintEqualToAnchor(window.bottomAnchor),
                    effectView.leadingAnchor.constraintEqualToAnchor(window.leadingAnchor),
                    effectView.trailingAnchor.constraintEqualToAnchor(window.trailingAnchor),
                ),
            )
            coverView = effectView
        }

        fun removeCover() {
            coverView?.removeFromSuperview()
            coverView = null
        }

        val resignObserver =
            notificationCenter.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = null,
            ) { addCover() }

        val becomeActiveObserver =
            notificationCenter.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = null,
            ) { removeCover() }

        onDispose {
            notificationCenter.removeObserver(resignObserver)
            notificationCenter.removeObserver(becomeActiveObserver)
            removeCover()
        }
    }
}
