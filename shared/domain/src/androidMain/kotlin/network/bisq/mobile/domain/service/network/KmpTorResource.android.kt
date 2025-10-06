package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.tor.common.api.ResourceLoader
import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec

actual fun torResourceLoader(resourceDir: File): ResourceLoader.Tor {
    return ResourceLoaderTorExec.getOrCreate(resourceDir)
}