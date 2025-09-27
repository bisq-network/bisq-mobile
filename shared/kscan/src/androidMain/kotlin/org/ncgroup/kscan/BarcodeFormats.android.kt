package org.ncgroup.kscan

/**
 * An object that contains constants for the different barcode formats and types that can be scanned.
 *
 * This object provides a set of predefined barcode formats and types that can be used to configure
 * the barcode scanner. Each constant represents a specific barcode format or type.
 *
 * The available barcode formats include:
 * - `FORMAT_CODE_128`: Code 128 barcode format.
 * - `FORMAT_CODE_39`: Code 39 barcode format.
 * - `FORMAT_CODE_93`: Code 93 barcode format.
 * - `FORMAT_CODABAR`: Codabar barcode format.
 * - `FORMAT_EAN_13`: EAN-13 barcode format.
 * - `FORMAT_EAN_8`: EAN-8 barcode format.
 * - `FORMAT_ITF`: ITF (Interleaved 2 of 5) barcode format.
 * - `FORMAT_UPC_A`: UPC-A barcode format.
 * - `FORMAT_UPC_E`: UPC-E barcode format.
 * - `FORMAT_QR_CODE`: QR Code barcode format.
 * - `FORMAT_PDF417`: PDF417 barcode format.
 * - `FORMAT_AZTEC`: Aztec barcode format.
 * - `FORMAT_DATA_MATRIX`: Data Matrix barcode format.
 * - `FORMAT_ALL_FORMATS`: A special format that includes all supported barcode formats.
 *
 * The available barcode types include:
 * - `TYPE_UNKNOWN`: Unknown barcode type.
 * - `TYPE_CONTACT_INFO`: Contact information barcode type.
 * - `TYPE_EMAIL`: Email barcode type.
 * - `TYPE_ISBN`: ISBN barcode type.
 * - `TYPE_PHONE`: Phone number barcode type.
 * - `TYPE_PRODUCT`: Product barcode type.
 * - `TYPE_SMS`: SMS barcode type.
 * - `TYPE_TEXT`: Text barcode type.
 * - `TYPE_URL`: URL barcode type.
 * - `TYPE_WIFI`: Wi-Fi network information barcode type.
 * - `TYPE_GEO`: Geographic location barcode type.
 * - `TYPE_CALENDAR_EVENT`: Calendar event barcode type.
 * - `TYPE_DRIVER_LICENSE`: Driver's license barcode type.
 */
actual object BarcodeFormats {
    actual val FORMAT_CODE_128: BarcodeFormat = BarcodeFormat.FORMAT_CODE_128
    actual val FORMAT_CODE_39: BarcodeFormat = BarcodeFormat.FORMAT_CODE_39
    actual val FORMAT_CODE_93: BarcodeFormat = BarcodeFormat.FORMAT_CODE_93
    actual val FORMAT_CODABAR: BarcodeFormat = BarcodeFormat.FORMAT_CODABAR
    actual val FORMAT_EAN_13: BarcodeFormat = BarcodeFormat.FORMAT_EAN_13
    actual val FORMAT_EAN_8: BarcodeFormat = BarcodeFormat.FORMAT_EAN_8
    actual val FORMAT_ITF: BarcodeFormat = BarcodeFormat.FORMAT_ITF
    actual val FORMAT_UPC_A: BarcodeFormat = BarcodeFormat.FORMAT_UPC_A
    actual val FORMAT_UPC_E: BarcodeFormat = BarcodeFormat.FORMAT_UPC_E
    actual val FORMAT_QR_CODE: BarcodeFormat = BarcodeFormat.FORMAT_QR_CODE
    actual val FORMAT_PDF417: BarcodeFormat = BarcodeFormat.FORMAT_PDF417
    actual val FORMAT_AZTEC: BarcodeFormat = BarcodeFormat.FORMAT_AZTEC
    actual val FORMAT_DATA_MATRIX: BarcodeFormat = BarcodeFormat.FORMAT_DATA_MATRIX
    actual val FORMAT_ALL_FORMATS: BarcodeFormat = BarcodeFormat.FORMAT_ALL_FORMATS
    actual val TYPE_UNKNOWN: BarcodeFormat = BarcodeFormat.TYPE_UNKNOWN
    actual val TYPE_CONTACT_INFO: BarcodeFormat = BarcodeFormat.TYPE_CONTACT_INFO
    actual val TYPE_EMAIL: BarcodeFormat = BarcodeFormat.TYPE_EMAIL
    actual val TYPE_ISBN: BarcodeFormat = BarcodeFormat.TYPE_ISBN
    actual val TYPE_PHONE: BarcodeFormat = BarcodeFormat.TYPE_PHONE
    actual val TYPE_PRODUCT: BarcodeFormat = BarcodeFormat.TYPE_PRODUCT
    actual val TYPE_SMS: BarcodeFormat = BarcodeFormat.TYPE_SMS
    actual val TYPE_TEXT: BarcodeFormat = BarcodeFormat.TYPE_TEXT
    actual val TYPE_URL: BarcodeFormat = BarcodeFormat.TYPE_URL
    actual val TYPE_WIFI: BarcodeFormat = BarcodeFormat.TYPE_WIFI
    actual val TYPE_GEO: BarcodeFormat = BarcodeFormat.TYPE_GEO
    actual val TYPE_CALENDAR_EVENT: BarcodeFormat = BarcodeFormat.TYPE_CALENDAR_EVENT
    actual val TYPE_DRIVER_LICENSE: BarcodeFormat = BarcodeFormat.TYPE_DRIVER_LICENSE
}
