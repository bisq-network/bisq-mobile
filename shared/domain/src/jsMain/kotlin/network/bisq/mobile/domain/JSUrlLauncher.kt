package network.bisq.mobile.domain

import network.bisq.mobile.domain.utils.Logging

import kotlinx.browser.window

class JSUrlLauncher : UrlLauncher, Logging {
    override fun openUrl(url: String) {
        log.d { "Opening URL in JS: $url" }
        kotlinx.browser.window.open(url, "_blank")
    }
}