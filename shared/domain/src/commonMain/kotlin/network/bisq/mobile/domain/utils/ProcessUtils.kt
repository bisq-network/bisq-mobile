package network.bisq.mobile.domain.utils

/**
 * view is Activity on android and MainView on iOS
 */
expect fun restartProcess(view: Any?)

/**
 * view is Activity on android and MainView on iOS
 */
expect fun killProcess(view: Any?)