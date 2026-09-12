package org.ncgroup.kscan

import zxingcpp.BarcodeReader.Format

/**
 * Maps between app [BarcodeFormat] and zxing-cpp [Format].
 */
internal object BarcodeFormatMapper {
    private val APP_TO_ZXING_FORMAT_MAP: Map<BarcodeFormat, Format> =
        mapOf(
            BarcodeFormat.FORMAT_QR_CODE to Format.QR_CODE,
            BarcodeFormat.FORMAT_CODE_128 to Format.CODE_128,
            BarcodeFormat.FORMAT_CODE_39 to Format.CODE_39,
            BarcodeFormat.FORMAT_CODE_93 to Format.CODE_93,
            BarcodeFormat.FORMAT_CODABAR to Format.CODABAR,
            BarcodeFormat.FORMAT_DATA_MATRIX to Format.DATA_MATRIX,
            BarcodeFormat.FORMAT_EAN_13 to Format.EAN_13,
            BarcodeFormat.FORMAT_EAN_8 to Format.EAN_8,
            BarcodeFormat.FORMAT_ITF to Format.ITF,
            BarcodeFormat.FORMAT_UPC_A to Format.UPC_A,
            BarcodeFormat.FORMAT_UPC_E to Format.UPC_E,
            BarcodeFormat.FORMAT_PDF417 to Format.PDF_417,
            BarcodeFormat.FORMAT_AZTEC to Format.AZTEC,
        )

    private val ZXING_TO_APP_FORMAT_MAP: Map<Format, BarcodeFormat> =
        APP_TO_ZXING_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }

    val allSupportedFormats: Set<Format> = APP_TO_ZXING_FORMAT_MAP.values.toSet()

    /**
     * Named rather than left empty for "all formats": an empty set asks zxing-cpp for every
     * symbology it knows, including ones that would only be decoded to be dropped as unknown.
     */
    fun toZxingFormats(appFormats: List<BarcodeFormat>): Set<Format> {
        if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return allSupportedFormats
        }
        return appFormats.mapNotNull { APP_TO_ZXING_FORMAT_MAP[it] }.toSet()
    }

    fun toAppFormat(zxingFormat: Format): BarcodeFormat =
        ZXING_TO_APP_FORMAT_MAP[zxingFormat]
            ?: ZXING_TO_APP_FORMAT_MAP[zxingFormat.symbology()]
            ?: BarcodeFormat.TYPE_UNKNOWN

    fun isKnownFormat(zxingFormat: Format): Boolean = toAppFormat(zxingFormat) != BarcodeFormat.TYPE_UNKNOWN

    /**
     * Whether a decoded barcode matches what the caller asked for. Requests that map to no
     * symbology (e.g. only TYPE_* values) match nothing, as with ML Kit.
     */
    fun isRequested(
        appFormat: BarcodeFormat,
        codeTypes: List<BarcodeFormat>,
    ): Boolean {
        if (codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return appFormat != BarcodeFormat.TYPE_UNKNOWN
        }
        return codeTypes.contains(appFormat)
    }

    /**
     * zxing-cpp encodes the symbology in the low byte and the variant in the high byte
     * (e.g. QR_CODE_MODEL_2 belongs to the QR_CODE symbology). Variant ' ' (0x20) is the family itself.
     */
    private fun Format.symbology(): Format? {
        val familyValue = (value and 0xFF) or SYMBOLOGY_FAMILY_VARIANT
        return Format.entries.firstOrNull { it.value == familyValue }
    }

    private const val SYMBOLOGY_FAMILY_VARIANT = 0x2000
}
