package org.ncgroup.kscan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controller for managing scanner functionalities like torch and zoom.
 *
 * This class provides a way to control the scanner's torch (flash) and zoom level.
 * It uses mutable state properties that can be observed for changes, allowing UI
 * updates in response to scanner state modifications.
 *
 * @property torchEnabled A boolean indicating whether the torch is currently enabled.
 *                        Defaults to `false`. Can be observed for changes.
 * @property zoomRatio The current zoom ratio of the scanner. Defaults to `1f`.
 *                     Can be observed for changes. The valid range is typically between 1f and `maxZoomRatio`.
 * @property maxZoomRatio The maximum zoom ratio supported by the scanner. Defaults to `1f`.
 *                        This property is read-only from outside the `kscan` package and is set internally.
 */
class ScannerController {
    var torchEnabled by mutableStateOf(false)

    var zoomRatio by mutableStateOf(1f)

    var maxZoomRatio by mutableStateOf(1f)
        internal set

    internal var onTorchChange: ((Boolean) -> Unit)? = null
    internal var onZoomChange: ((Float) -> Unit)? = null

    fun setTorch(enabled: Boolean) {
        onTorchChange?.invoke(enabled)
    }

    fun setZoom(ratio: Float) {
        onZoomChange?.invoke(ratio)
    }
}
