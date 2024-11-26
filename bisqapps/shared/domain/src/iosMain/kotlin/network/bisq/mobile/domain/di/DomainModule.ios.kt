package network.bisq.mobile.domain.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

@OptIn(ExperimentalSettingsImplementation::class)
actual fun provideSettings(): Settings {
    return KeychainSettings("Settings")
}