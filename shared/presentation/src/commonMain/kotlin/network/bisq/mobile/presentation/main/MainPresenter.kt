package network.bisq.mobile.presentation.main

import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.data.utils.getDeviceLanguageCode
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import kotlin.coroutines.cancellation.CancellationException

/**
 * Main Presenter as an example of implementation for now.
 * This class should be abstract, kept concrete for testing purposes
 */
@OptIn(ExperimentalCoroutinesApi::class)
open class MainPresenter(
    tradesServiceFacade: TradesServiceFacade,
    val userProfileServiceFacade: UserProfileServiceFacade,
    private val openTradesNotificationService: OpenTradesNotificationService,
    private val settingsService: SettingsServiceFacade,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val urlLauncher: UrlLauncher,
    protected val applicationLifecycleService: ApplicationLifecycleService,
) : BasePresenter(null),
    AppPresenter {
    // Observable state
    private val _isMainContentVisible = MutableStateFlow(false)
    override val isMainContentVisible: StateFlow<Boolean> = _isMainContentVisible.asStateFlow()

    private val _isSmallScreen = MutableStateFlow(false)
    override val isSmallScreen: StateFlow<Boolean> = _isSmallScreen.asStateFlow()

    protected val _showAllConnectionsLostDialogue = MutableStateFlow(false)
    override val showAllConnectionsLostDialogue: StateFlow<Boolean> = _showAllConnectionsLostDialogue.asStateFlow()

    protected val _showReconnectOverlay = MutableStateFlow(false)
    override val showReconnectOverlay: StateFlow<Boolean> = _showReconnectOverlay.asStateFlow()

    open val reconnectOverlayInfoKey: String = "mobile.connectivity.reconnecting.info"
    open val reconnectOverlayDetailsKey: String = "mobile.connectivity.reconnecting.details"

    open val reconnectOverlayButtonKey: String = "mobile.connectivity.reconnecting.restart"

    open val connectionsLostDialogTitleKey: String = "mobile.connectivity.disconnected.title"
    open val connectionsLostDialogMessageKey: String = "mobile.connectivity.disconnected.message"

    final override val languageCode: StateFlow<String> get() = settingsService.languageCode

    var view: Any? = null
        private set

    // TODO: refactor when TradeItemPresentationModel is completely immutable
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> =
        tradesServiceFacade.openTradeItems
            .flatMapLatest { openTradeItems ->
                val flowsList: List<Flow<Pair<String, Int>>> =
                    openTradeItems.map { trade ->
                        combine(
                            trade.bisqEasyOpenTradeChannelModel.chatMessages,
                            tradeReadStateRepository.data.map { it.map },
                            userProfileServiceFacade.ignoredProfileIds,
                        ) { messages, readStates, ignoredIds ->
                            // TODO: refactor to filter visible messages based on ignore at trade.bisqEasyOpenTradeChannelModel.chatMessages for consistency
                            // this is a duplicated logic from OpenTradePresenter msgCount collection
                            val visibleMessages =
                                messages.filter {
                                    when (it.chatMessageType) {
                                        ChatMessageTypeEnum.TEXT, ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER -> it.senderUserProfileId !in ignoredIds
                                        else -> true
                                    }
                                }
                            val unread =
                                (visibleMessages.size - readStates.getOrElse(trade.tradeId) { 0 }).coerceAtLeast(
                                    0,
                                )
                            trade.tradeId to unread
                        }
                    }
                if (flowsList.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(flowsList) { pairs: Array<Pair<String, Int>> ->
                        pairs
                            .filter { it.second > 0 }
                            .associate { it.first to it.second }
                    }
                }
            }.flowOn(Dispatchers.Default)
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                emptyMap(),
            )

    override val showAnimation: StateFlow<Boolean> get() = settingsService.useAnimations

    init {
        val localeCode = getDeviceLanguageCode()
        val screenWidth = getScreenWidthDp()
        _isSmallScreen.value = screenWidth < 480
        log.i { "Shared Version: ${BuildConfig.SHARED_LIBS_VERSION}" }
        log.i { "iOS Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Client Version: ${BuildConfig.ANDROID_APP_VERSION}" }
        log.i { "Android Node Version: ${BuildNodeConfig.APP_VERSION}" }
        log.i { "Device language code: $localeCode" }
        log.i { "Screen width: $screenWidth" }
        log.i { "Small screen: ${_isSmallScreen.value}" }
    }

    fun attachView(view: Any) {
        // at the moment the attach view is with the activity/ main view in ios
        // unless we change this there is no point in sharing with dependents
        this.view = view
        log.i { "Lifecycle: Main View attached to Main Presenter" }
    }

    fun detachView() {
        onViewUnattaching()
        this.view = null
        log.i { "Lifecycle: View Dettached from Presenter" }
    }

    @CallSuper
    override fun onViewAttached() {
        super.onViewAttached()

        log.i { "Lifecycle: View ${if (view != null) view!!::class.simpleName else ""} attached to presenter ${this::class.simpleName}" }

        languageCode
            .filter { it.isNotEmpty() }
            .onEach {
                settingsService.setLanguageCode(it)
            }.take(1)
            .launchIn(presenterScope)

        startLanguageAnalyticsObserverOnce()
    }

    private var languageAnalyticsObserverStarted = false

    /**
     * Emit [AnalyticsEvent.Settings.LanguageChanged] whenever the observed UI
     * language settles on a new code. Single-shot per attached scope —
     * MainPresenter is a Koin `single`, so this guards against double-emission
     * when [onViewAttached] is invoked multiple times on the SAME live
     * `presenterScope` (background→foreground without a config change).
     *
     * The guard is RESET in [onViewUnattaching] because
     * `jobsManager.dispose()` cancels the old scope and creates a fresh one
     * (e.g. on Android config change → activity recreation). Without the
     * reset, the next `onViewAttached` would skip on the stale flag and the
     * collector would never start on the new scope.
     *
     * Emission semantics (matches the rationale rodvar locked in 2026-06-12 —
     * "most important so we get to know our userbase preferences on languages"):
     *  - First non-blank value after the collector starts → auto-detected
     *    baseline, captures the "user has language X" baseline distribution.
     *  - Each subsequent distinct value → explicit user change.
     *
     * Untracked codes (e.g. a future translation we haven't whitelisted yet)
     * are NOT emitted — `TRACKED_LANGUAGE_CODES` is the wire-format allowlist.
     * Defence against a backend that hands us an unexpected code AND
     * defence against typos in the code path.
     *
     * `analyticsService?` is null in tests that don't bind it — silent no-op
     * by design (see [analyticsService] kdoc on [BasePresenter]).
     */
    private fun startLanguageAnalyticsObserverOnce() {
        if (languageAnalyticsObserverStarted) return
        languageAnalyticsObserverStarted = true

        languageCode
            .mapNotNull { AnalyticsEvent.Settings.normalizeLanguageCode(it) }
            .distinctUntilChanged()
            .onEach { code ->
                analyticsService?.track(AnalyticsEvent.Settings.LanguageChanged(code))
            }.launchIn(presenterScope)
    }

    @CallSuper
    override fun onViewUnattaching() {
        // Reset the guard FIRST — `super.onViewUnattaching()` triggers
        // `jobsManager.dispose()` which cancels the current scope and creates
        // a new one. Next `onViewAttached` needs to start a fresh collector
        // on that new scope; with the flag still `true`, it'd be skipped.
        languageAnalyticsObserverStarted = false
        super.onViewUnattaching()
    }

    @CallSuper
    override fun onDestroying() {
        globalUiManager.dispose()
        cleanupNotificationService()
        super.onDestroying()
    }

    open fun cleanupNotificationService() {
        if (isIOS()) {
            // Fire-and-forget on Default dispatcher to avoid blocking the main thread.
            // Using runBlocking here caused iOS CA Fence hangs during view teardown.
            CoroutineScope(Dispatchers.Default).launch {
                runCatching {
                    openTradesNotificationService.stopNotificationService()
                }.onFailure { e ->
                    log.w(e) { "Failed to stop notification service during iOS cleanup" }
                }
            }
        } else {
            cleanupNotificationServiceSync()
        }
    }

    protected fun cleanupNotificationServiceSync() {
        // to stop notification service and fully kill app (no zombie mode)
        runBlocking {
            openTradesNotificationService.stopNotificationService()
        }
    }

    override fun setIsMainContentVisible(value: Boolean) {
        _isMainContentVisible.value = value
    }

    /**
     * Opens [url] in the system browser (suspend — awaits platform result). On failure or
     * unexpected launcher errors, shows [mobile.error.cannotOpenUrl] once — callers should not
     * duplicate that snackbar.
     */
    suspend fun navigateToUrlWithLauncher(url: String): Boolean =
        try {
            val opened = urlLauncher.openUrl(url)
            if (!opened) {
                showCannotOpenUrlSnackbar()
            }
            opened
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed to navigate to URL: $url" }
            showCannotOpenUrlSnackbar()
            false
        }

    private fun showCannotOpenUrlSnackbar() {
        showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
    }

    override fun onCloseConnectionLostDialogue() {
        _showAllConnectionsLostDialogue.value = false
    }

    override fun onRestartApp() {
        applicationLifecycleService.restartApp(view)
    }

    open fun onConnectivityRecoveryAction() {
        onRestartApp()
    }

    override fun onTerminateApp() {
        applicationLifecycleService.terminateApp(view)
    }

    override fun isDemo(): Boolean = false

    /**
     * Common error handling method for initialization and service activation errors.
     * Provides user-friendly error messages with contextual guidance.
     *
     * @param exception The exception that occurred
     * @param context Additional context about where the error occurred (e.g., "Node initialization", "Service activation")
     */
    protected fun handleInitializationError(
        exception: Throwable,
        context: String = "Initialization",
    ) {
        // Use the existing error handling infrastructure
        presenterScope.launch {
            GenericErrorHandler.handleGenericError(
                "Initialization process failed during: $context",
                exception,
            )
        }
    }
}
