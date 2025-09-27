package org.ncgroup.kscan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A Composable function that displays the scanner UI.
 *
 * @param onCancel A callback function that is invoked when the cancel button is clicked.
 * @param torchEnabled A boolean value that indicates whether the torch is enabled.
 * @param onTorchEnabled A callback function that is invoked when the torch button is clicked.
 * @param zoomRatio A float value that represents the current zoom ratio.
 * @param zoomRatioOnChange A callback function that is invoked when the zoom ratio changes.
 * @param maxZoomRatio A float value that represents the maximum zoom ratio.
 * @param colors An optional [ScannerColors] object that specifies the colors for the scanner UI.
 */
@Composable
fun ScannerUI(
    onCancel: () -> Unit,
    torchEnabled: Boolean,
    onTorchEnabled: (Boolean) -> Unit,
    zoomRatio: Float,
    zoomRatioOnChange: (Float) -> Unit,
    maxZoomRatio: Float,
    colors: ScannerColors = scannerColors(),
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScannerHeader(
            onCancel = onCancel,
            torchEnabled = torchEnabled,
            onTorchEnabled = onTorchEnabled,
            containerColor = colors.headerContainerColor,
            navigationIconColor = colors.headerNavigationIconColor,
            titleColor = colors.headerTitleColor,
            actionIconColor = colors.headerActionIconColor,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(10.dp))

        ScannerBarcodeFrame(
            frameColor = colors.barcodeFrameColor,
        )

        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.weight(1f))

//        ScannerZoomAdjuster(
//            modifier = Modifier.padding(bottom = 5.dp),
//            zoomRatio = zoomRatio,
//            zoomRatioOnChange = zoomRatioOnChange,
//            maxZoomRatio = maxZoomRatio,
//            containerColor = colors.zoomControllerContainerColor,
//            contentColor = colors.zoomControllerContentColor,
//        )
    }
}
