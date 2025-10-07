package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.tor.runtime.TorRuntime

expect fun torRuntimeEnvironment(baseDirPath: String?): TorRuntime.Environment
