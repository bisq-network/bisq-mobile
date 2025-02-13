package network.bisq.mobile.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.interop.LocalUIViewController
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.navigationController
import platform.darwin.NSObject

@Composable
actual fun BackHandler(onBackPressed: () -> Unit) {
    val viewController = LocalUIViewController.current

    DisposableEffect(viewController) {
        val navigationController = viewController.navigationController

        val backPressHandler = object : NSObject(), UINavigationControllerDelegateProtocol {
            override fun navigationController(
                navigationController: UINavigationController,
                didShowViewController: UIViewController,
                animated: Boolean
            ) {
                if (navigationController.viewControllers.contains(viewController).not()) {
                    onBackPressed()
                }
            }
        }

        navigationController?.delegate = backPressHandler

        onDispose {
            if (navigationController?.delegate == backPressHandler) {
                navigationController?.delegate = null
            }
        }
    }
}
