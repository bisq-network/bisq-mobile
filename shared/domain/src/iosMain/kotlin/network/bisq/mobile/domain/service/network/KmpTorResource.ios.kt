package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.tor.common.api.ResourceLoader
import io.matthewnelson.kmp.tor.resource.noexec.tor.ResourceLoaderTorNoExec

actual fun torResourceLoader(
    resourceDir: File,
): ResourceLoader.Tor {
    return ResourceLoaderTorNoExec.getOrCreate(resourceDir)
}