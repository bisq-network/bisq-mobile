package org.ncgroup.kscan

import zxingcpp.BarcodeReader.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeFormatMapperTest {
    @Test
    fun `GIVEN empty list WHEN toZxingFormats THEN returns all supported formats`() {
        assertEquals(BarcodeFormatMapper.allSupportedFormats, BarcodeFormatMapper.toZxingFormats(emptyList()))
    }

    @Test
    fun `GIVEN all formats WHEN toZxingFormats THEN returns all supported formats`() {
        assertEquals(
            BarcodeFormatMapper.allSupportedFormats,
            BarcodeFormatMapper.toZxingFormats(listOf(BarcodeFormat.FORMAT_ALL_FORMATS)),
        )
    }

    @Test
    fun `GIVEN multiple formats WHEN toZxingFormats THEN returns zxing formats`() {
        val result =
            BarcodeFormatMapper.toZxingFormats(
                listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13, BarcodeFormat.TYPE_URL),
            )

        assertEquals(setOf(Format.QR_CODE, Format.EAN_13), result)
    }

    @Test
    fun `GIVEN exact zxing format WHEN toAppFormat THEN returns app format`() {
        assertEquals(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormatMapper.toAppFormat(Format.QR_CODE))
        assertEquals(BarcodeFormat.FORMAT_EAN_13, BarcodeFormatMapper.toAppFormat(Format.EAN_13))
        assertEquals(BarcodeFormat.FORMAT_UPC_A, BarcodeFormatMapper.toAppFormat(Format.UPC_A))
        assertEquals(BarcodeFormat.FORMAT_PDF417, BarcodeFormatMapper.toAppFormat(Format.PDF_417))
    }

    @Test
    fun `GIVEN symbology variant WHEN toAppFormat THEN returns family app format`() {
        assertEquals(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormatMapper.toAppFormat(Format.QR_CODE_MODEL_2))
        assertEquals(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormatMapper.toAppFormat(Format.MICRO_QR_CODE))
        assertEquals(BarcodeFormat.FORMAT_CODE_39, BarcodeFormatMapper.toAppFormat(Format.CODE_39_EXT))
        assertEquals(BarcodeFormat.FORMAT_ITF, BarcodeFormatMapper.toAppFormat(Format.ITF_14))
        assertEquals(BarcodeFormat.FORMAT_PDF417, BarcodeFormatMapper.toAppFormat(Format.COMPACT_PDF_417))
    }

    @Test
    fun `GIVEN unsupported zxing format WHEN toAppFormat THEN returns unknown`() {
        assertEquals(BarcodeFormat.TYPE_UNKNOWN, BarcodeFormatMapper.toAppFormat(Format.MAXI_CODE))
        assertEquals(BarcodeFormat.TYPE_UNKNOWN, BarcodeFormatMapper.toAppFormat(Format.ISBN))
        assertEquals(BarcodeFormat.TYPE_UNKNOWN, BarcodeFormatMapper.toAppFormat(Format.NONE))
    }

    @Test
    fun `GIVEN formats WHEN isKnownFormat THEN reflects mapping`() {
        assertTrue(BarcodeFormatMapper.isKnownFormat(Format.QR_CODE_MODEL_1))
        assertFalse(BarcodeFormatMapper.isKnownFormat(Format.DX_FILM_EDGE))
    }
}
