package network.bisq.mobile.client.web.services

import network.bisq.mobile.domain.UrlLauncher

class WebUrlLauncher : UrlLauncher {
    override fun openUrl(url: String) {
        js("window.open(url, '_blank')")
    }
}