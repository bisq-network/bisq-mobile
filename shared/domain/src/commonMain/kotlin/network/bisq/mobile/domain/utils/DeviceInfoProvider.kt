package network.bisq.mobile.domain.utils

interface DeviceInfoProvider {
    fun getDeviceInfo(): String

    /**
     * Total physical RAM in bytes as reported by the OS. This is always LESS than the nominal
     * capacity because the kernel/firmware reserve a slice, so a "3GB" phone reports ~2.7–2.9 GiB
     * and a "4GB" phone reports ~3.6–3.8 GiB. Returns 0 when it cannot be determined.
     */
    fun getTotalRamBytes(): Long

    /**
     * True when the device is at/under the low-spec RAM ceiling where heavy Compose navigation
     * transitions cause heap pressure / ANRs on the offerbook, so animations are
     * force-disabled. The [LOW_SPEC_RAM_THRESHOLD_BYTES] sits between a nominal-3GB device's
     * reported total and a nominal-4GB device's, so "3GB or less" is caught and 4GB+ is left alone.
     * Unknown RAM (0) is treated as NOT low-spec — we don't disable animations on a device we
     * simply couldn't measure.
     */
    fun isLowSpecDevice(): Boolean = getTotalRamBytes() in 1 until LOW_SPEC_RAM_THRESHOLD_BYTES

    companion object {
        // ~3.2 GiB. Above a nominal-3GB device's reported RAM (~2.8 GiB), below a nominal-4GB
        // device's (~3.7 GiB). Used to force animations off at 3GB or less.
        const val LOW_SPEC_RAM_THRESHOLD_BYTES = 3_435_973_837L
    }
}
