package network.bisq.mobile.client.di

import network.bisq.mobile.client.service.user_profile.ClientCatHashService
import network.bisq.mobile.domain.AndroidUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.service.AndroidClientCatHashService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module


val androidClientModule = module {
    single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }
    single {
        val context = androidContext()
        val filesDir = context.filesDir.absolutePath
        AndroidClientCatHashService(context, filesDir)
    } bind ClientCatHashService::class
}