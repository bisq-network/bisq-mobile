package network.bisq.mobile.domain

import network.bisq.mobile.domain.utils.Logging

class JSUrlLauncher : UrlLauncher, Logging {
    override fun openUrl(url: String) {
        log.d { "Opening URL in JS: $url" }
        // TODO use something like Kotlin/JS interop to open the URL in the browser. For example: window.open(url, "_blank")
    }
}