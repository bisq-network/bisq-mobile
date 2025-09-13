package network.bisq.mobile.client.service.settings

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.utils.SemanticVersion

object SettingsUtils {

    fun getRequiredApiVersion() = BuildConfig.BISQ_API_VERSION

    /**
     * Checks if the provided API version is compatible with the required version.
     * Won't throw if the `apiVersion` cannot be parsed.
     *
     * @param apiVersion The API version string to check (must be a valid semantic version).
     * @return True if the API version is valid and greater than or equal to the required version
     */
    fun isApiCompatible(apiVersion: String): Boolean {
        return runCatching {
            val requiredVersion = getRequiredApiVersion()
            val apiSemVer = SemanticVersion.from(apiVersion)
            val requiredSemVer = SemanticVersion.from(requiredVersion)
            return apiSemVer >= requiredSemVer
        }.getOrElse { false }
    }
}
