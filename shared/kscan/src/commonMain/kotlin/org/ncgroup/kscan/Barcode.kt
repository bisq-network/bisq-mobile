package org.ncgroup.kscan

/**
 * Represents a scanned barcode.
 *
 * @property data The decoded data from the barcode.
 * @property format The format of the barcode (e.g., QR_CODE, EAN_13).
 */
data class Barcode(
    val data: String,
    val format: String,
)
