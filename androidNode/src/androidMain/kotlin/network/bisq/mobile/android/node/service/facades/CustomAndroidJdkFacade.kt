package network.bisq.mobile.android.node.service.facades

import bisq.common.facades.android.AndroidJdkFacade
import network.bisq.mobile.domain.utils.Logging
import java.util.stream.Stream

/**
 * Custom AndroidJdkFacade that properly implements getProcessCommandStream for Android
 * 
 * The original AndroidJdkFacade.getProcessCommandStream() throws UnsupportedOperationException,
 * which causes Bisq's TorService.isTorRunning() to fail. This custom implementation provides
 * a working version that returns an empty stream, indicating no Tor process is found.
 */
class CustomAndroidJdkFacade(pid: Int) : AndroidJdkFacade(pid), Logging {
    
    override fun getProcessCommandStream(): Stream<String>? {
        return Stream.of()
    }
}