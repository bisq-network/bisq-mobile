package org.ncgroup.kscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    colors: ScannerColors,
    showUi: Boolean,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }


    val captureDevice: AVCaptureDevice? =
        remember {
            AVCaptureDevice.defaultDeviceWithDeviceType(
                AVCaptureDeviceTypeBuiltInWideAngleCamera,
                AVMediaTypeVideo,
                AVCaptureDevicePositionBack,
            )
        }

    if (captureDevice == null) {
        result(BarcodeResult.OnFailed(Exception("No back camera available")))
        return
    }

    scannerController?.onTorchChange = { enabled ->
        val prev = torchEnabled
        runCatching {
            if (captureDevice.hasTorch) {
                captureDevice.lockForConfiguration(null)
                captureDevice.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                captureDevice.unlockForConfiguration()
                torchEnabled = enabled
                scannerController.torchEnabled = enabled
            }
        }.onFailure { e ->
            // Revert state and report
            torchEnabled = prev
            scannerController.torchEnabled = prev
            result(BarcodeResult.OnFailed(Exception(e.message ?: "Torch toggle failed")))
        }
    }

    val controller = remember {
        CameraViewController(
            device = captureDevice,
            codeTypes = codeTypes,
            filter = filter,
            onBarcodeSuccess = { scannedBarcodes ->
                result(BarcodeResult.OnSuccess(scannedBarcodes.first()))
            },
            onBarcodeFailed = { error ->
                result(BarcodeResult.OnFailed(error))
            },
            onBarcodeCanceled = {
                result(BarcodeResult.OnCanceled)
            },
            onMaxZoomRatioAvailable = { maxRatio ->
                maxZoomRatio = maxRatio
            }
        )
    }

    scannerController?.onZoomChange = { ratio ->
        controller.setZoom(ratio)
        zoomRatio = ratio
        scannerController.zoomRatio = ratio
    }

    scannerController?.maxZoomRatio = maxZoomRatio

    Box(modifier = modifier) {
        UIKitViewController(
            factory = { controller },
            modifier = Modifier.matchParentSize(),
        )

        if (showUi) {
            ScannerUI(
                onCancel = {
                    controller.dispose()
                    result(BarcodeResult.OnCanceled)
                },
                torchEnabled = torchEnabled,
                onTorchEnabled = { enabled ->
                    val prev = torchEnabled
                    runCatching {
                        if (captureDevice.hasTorch) {
                            captureDevice.lockForConfiguration(null)
                            captureDevice.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                            captureDevice.unlockForConfiguration()
                            torchEnabled = enabled
                        }
                    }.onFailure { e ->
                        // Revert state and report
                        torchEnabled = prev
                        scannerController?.torchEnabled = prev
                        result(BarcodeResult.OnFailed(Exception(e.message ?: "Torch toggle failed")))
                    }
                },
                zoomRatio = zoomRatio,
                zoomRatioOnChange = { ratio ->
                    controller.setZoom(ratio)
                    zoomRatio = ratio
                },
                maxZoomRatio = maxZoomRatio,
                colors = colors,
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.dispose()
        }
    }
}
