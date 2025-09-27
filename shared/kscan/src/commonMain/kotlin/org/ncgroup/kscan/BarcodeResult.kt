package org.ncgroup.kscan

/**
 * Represents the result of a barcode scanning operation.
 * It can be one of three states:
 * - [OnSuccess]: Indicates that a barcode was successfully scanned.
 * - [OnFailed]: Indicates that the barcode scanning failed due to an error.
 * - [OnCanceled]: Indicates that the barcode scanning was canceled by the user.
 */
sealed interface BarcodeResult {
    data class OnSuccess(val barcode: Barcode) : BarcodeResult

    data class OnFailed(val exception: Exception) : BarcodeResult

    data object OnCanceled : BarcodeResult
}
