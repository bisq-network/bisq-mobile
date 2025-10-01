package network.bisq.mobile.presentation

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.getDeviceLanguageCode
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.SystemOutFilter
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.notification.NotificationChannels
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Base class for Bisq Android Applications
 */
abstract class MainApplication : Application(), Logging {
    override fun onCreate() {
        super.onCreate()

        setupI18n()
        setupSystemOutFiltering()
        setupKoinDI(this)
        createNotificationChannels()
        onCreated()
    }

    private fun setupI18n() {
        // Initialize early with users device language. Later once settings are available we update if user has changed language.
        val deviceLanguageCode = getDeviceLanguageCode()
        I18nSupport.initialize(deviceLanguageCode)
        setDefaultLocale(deviceLanguageCode)
    }

    protected fun setupKoinDI(appContext: Context) {
        startKoin {
            androidContext(appContext)
            modules(getKoinModules())
        }
    }

    protected abstract fun getKoinModules(): List<Module>

    protected open fun onCreated() {
        // default impl
    }

    protected open fun isDebug(): Boolean {
        return BuildConfig.IS_DEBUG
    }

    /**
     * Sets up System.out filtering using the shared SystemOutFilter utility.
     * This blocks verbose System.out.println() calls from Bisq2 JARs that bypass the logging framework.
     */
    private fun setupSystemOutFiltering() {
        SystemOutFilter.setupSystemOutFiltering(
            isDebugBuild = isDebug(),
            completeBlockInRelease = true
        )
        log.i { "System.out filtering configured for ${if (isDebug()) "debug" else "release"} build" }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NotificationChannels.BISQ_SERVICE,
                "mobile.android.channels.service".i18n(),
                NotificationManager.IMPORTANCE_DEFAULT // Default importance to avoid OS killing the app
            ).apply {
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
                // trick to have a sound but have it silent, as it's required for IMPORTANCE_DEFAULT
                val soundUri = ResourceUtils.getSoundUri(applicationContext, "silent.mp3")
                if (soundUri != null) {
                    val audioAttributes =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    setSound(soundUri, audioAttributes)
                } else {
                    log.w { "Unable to retrieve silent.mp3 sound uri for service channel" }
                }
            }

            val tradeUpdatesChannel = NotificationChannel(
                NotificationChannels.TRADE_UPDATES,
                "mobile.android.channels.tradeState".i18n(),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(false)
                enableVibration(true)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }

            val userMessagesChannel = NotificationChannel(
                NotificationChannels.USER_MESSAGES,
                "mobile.android.channels.userMessages".i18n(),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(false)
                enableVibration(true)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }

            val manager =
                NotificationManagerCompat.from(applicationContext)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(tradeUpdatesChannel)
            manager.createNotificationChannel(userMessagesChannel)
            log.i { "Created notification channels" }
        }
    }
} 