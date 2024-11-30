package network.bisq.mobile

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

expect class PlatformImage

expect fun ByteArray.toImageBitmap(): PlatformImage

expect fun PlatformImage.toByteArray(): ByteArray