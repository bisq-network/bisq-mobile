package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.tor.common.api.ResourceLoader

expect fun torResourceLoader(
    resourceDir: File,
): ResourceLoader.Tor