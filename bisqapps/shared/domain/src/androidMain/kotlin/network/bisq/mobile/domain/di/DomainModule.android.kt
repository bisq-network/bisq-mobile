package network.bisq.mobile.domain.di

import com.russhwolf.settings.Settings

actual fun provideSettings(): Settings {
     return Settings()
}