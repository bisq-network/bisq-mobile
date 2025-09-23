package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.IOSUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.utils.ClientVersionProvider
import network.bisq.mobile.domain.utils.VersionProvider
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientModule = module {
    single<UrlLauncher> { IOSUrlLauncher() }
    single<VersionProvider> { ClientVersionProvider() }
}
