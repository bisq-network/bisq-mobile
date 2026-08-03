package network.bisq.mobile.test.presentation.coroutines

abstract class PlatformPresentationKoinTestBase : PresentationKoinTestBase() {
    override fun setUpPlatformMocks() {
        PlatformStaticMocks.mockScreenWidth(480)
    }

    override fun tearDownPlatformMocks() {
        PlatformStaticMocks.unmockScreenWidth()
    }
}
