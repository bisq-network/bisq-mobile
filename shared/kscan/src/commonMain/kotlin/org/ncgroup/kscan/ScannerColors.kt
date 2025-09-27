package org.ncgroup.kscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ScannerColors(
    val headerContainerColor: Color = Color(0xFF291544),
    val headerNavigationIconColor: Color = Color.White,
    val headerTitleColor: Color = Color.White,
    val headerActionIconColor: Color = Color.White,
    val zoomControllerContainerColor: Color = Color(0xFF291544),
    val zoomControllerContentColor: Color = Color.White,
    val barcodeFrameColor: Color = Color(0xFFF050F8),
)

/**
 * Creates a [ScannerColors] instance with the specified colors.
 *
 * This function allows for customization of the scanner's visual appearance by providing
 * specific colors for different components. If a color is not specified, it will default
 * to the predefined values.
 *
 * @param headerContainerColor The background color of the header.
 * @param headerNavigationIconColor The color of the navigation icon in the header.
 * @param headerTitleColor The color of the title text in the header.
 * @param headerActionIconColor The color of the action icon in the header.
 * @param zoomControllerContainerColor The background color of the zoom controller.
 * @param zoomControllerContentColor The color of the content (e.g., icons, text) within the zoom controller.
 * @param barcodeFrameColor The color of the frame that highlights the detected barcode.
 * @return A [ScannerColors] object containing the specified colors.
 */
@Composable
fun scannerColors(
    headerContainerColor: Color = Color(0xFF291544),
    headerNavigationIconColor: Color = Color.White,
    headerTitleColor: Color = Color.White,
    headerActionIconColor: Color = Color.White,
    zoomControllerContainerColor: Color = Color(0xFF291544),
    zoomControllerContentColor: Color = Color.White,
    barcodeFrameColor: Color = Color(0xFFF050F8),
): ScannerColors {
    return ScannerColors(
        headerContainerColor = headerContainerColor,
        headerNavigationIconColor = headerNavigationIconColor,
        headerTitleColor = headerTitleColor,
        headerActionIconColor = headerActionIconColor,
        zoomControllerContainerColor = zoomControllerContainerColor,
        zoomControllerContentColor = zoomControllerContentColor,
        barcodeFrameColor = barcodeFrameColor,
    )
}
