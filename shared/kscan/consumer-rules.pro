# Consumer ProGuard rules for the KScan library
# Packaged with the AAR and applied by consuming apps in release builds.

# zxing-cpp and CameraX ship their own keep rules for what they load by name
# (zxing-cpp's JNI classes, CameraX's Camera2Config provider and quirks), so
# none are needed here.
-dontwarn androidx.camera.**

# Prevent obfuscation of KScan public API to keep binary compatibility
-keep class org.ncgroup.kscan.** { *; }
