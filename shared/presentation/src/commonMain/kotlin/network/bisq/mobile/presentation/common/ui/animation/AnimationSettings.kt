package network.bisq.mobile.presentation.common.ui.animation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.DeviceInfoProvider

/**
 * Single source of truth for whether UI animations are *effectively* enabled.
 *
 * effective = the user's `useAnimations` setting AND the device is not low-spec.
 *
 * On low-spec devices ([DeviceInfoProvider.isLowSpecDevice], ~3GB RAM or less) heavy Compose
 * navigation transitions cause heap pressure / ANRs on the offerbook, so animations
 * are force-disabled and the settings toggle is greyed out. This is an EFFECTIVE override: the
 * user's stored preference is left untouched, so it takes effect again if the app ever runs on a
 * capable device — we never mutate persisted settings here.
 *
 * The device lock only applies to the Node app ([applyDeviceLock] = true). Bisq Connect is a
 * lightweight client with no embedded node, so it has no such heap pressure — there animations
 * always follow the user setting only ([applyDeviceLock] = false).
 *
 * Read [enabled] everywhere an animation should honour the setting, instead of
 * `settingsServiceFacade.useAnimations` directly.
 */
class AnimationSettings(
    settingsServiceFacade: SettingsServiceFacade,
    deviceInfoProvider: DeviceInfoProvider,
    applyDeviceLock: Boolean,
) {
    /** True when the device forces animations off regardless of the user setting (toggle greyed). */
    val lockedByDevice: Boolean = applyDeviceLock && deviceInfoProvider.isLowSpecDevice()

    // Constant "off" flow used when the device lock is active. No coroutine/scope needed: when not
    // locked we simply pass the user setting through unchanged.
    private val forcedOff = MutableStateFlow(false)

    val enabled: StateFlow<Boolean> =
        if (lockedByDevice) forcedOff else settingsServiceFacade.useAnimations

    /**
     * The effective animations-enabled value for a given stored [useAnimations] preference.
     *
     * Single source of truth for the "user setting AND not device-locked" rule, matching what
     * [enabled] exposes. Use this when computing effective state from a freshly-read preference
     * (e.g. a settings screen) instead of re-deriving `useAnimations && !lockedByDevice` inline.
     */
    fun isEffectivelyEnabled(useAnimations: Boolean): Boolean = useAnimations && !lockedByDevice
}
