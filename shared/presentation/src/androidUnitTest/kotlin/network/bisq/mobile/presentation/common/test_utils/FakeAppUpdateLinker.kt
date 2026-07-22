package network.bisq.mobile.presentation.common.test_utils

import network.bisq.mobile.data.utils.AppUpdateLinker

/** Sentinel URL for presenter tests — must not match production release destinations. */
const val TEST_APP_UPDATE_URL = "https://example.test/bisq-app-update"

class FakeAppUpdateLinker(
    private val updateUrl: String = TEST_APP_UPDATE_URL,
) : AppUpdateLinker {
    override fun getUpdateUrl(): String = updateUrl
}
