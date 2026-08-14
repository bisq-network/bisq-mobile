# Bisq Connect proguard file
#
# Obfuscation OFF deliberately and stays off: Bisq is AGPL open source, so renaming buys no
# secrecy — that holds for MuSig too, whose security lives in node-side custody and
# keystore-held credentials, not name mangling. Readable production stack traces
# (GlitchTip/Play) matter more, and we run no mapping-upload pipeline (removed in #1695).
-dontobfuscate

# Optimization ON — deliberately diverging from the node app: Connect ships no bisq2 jars, so
# the node's lambda-classname registry landmines (its reason for -dontoptimize) do not exist
# here. Reflective surfaces are pinned by the targeted keeps below. Verify on any R8/AGP bump:
# websocket JSON round-trip, Tor bootstrap, QR scan, push-notification key decrypt.
# Optimization also activates the -assumenosideeffects log-stripping rules at the bottom.
#-dontoptimize

########################################
# Kotlinx Serialization — the app's wire format (websocket JSON to the trusted node),
# datastore persistence, and type-safe Compose navigation routes all resolve serializers
# reflectively.
########################################

-keep class **$$serializer { *; }
-keepclassmembers class **$Companion {
    public static kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    public static kotlinx.serialization.KSerializer serializer(...);
}
-keepnames @kotlinx.serialization.Serializable class ** { *; }

# Critical R8 full mode rule for sealed class serialization
# See: https://github.com/Kotlin/kotlinx.serialization/issues/2050
-if @kotlinx.serialization.Serializable class **
-keep, allowshrinking, allowoptimization, allowobfuscation, allowaccessmodification class <1>

# Keep attributes needed for polymorphic serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Explicitly keep sealed interface implementations for offer specs (polymorphic JSON over the
# websocket — same rule as the node app)
-keep class network.bisq.mobile.data.replicated.offer.amount.spec.** { *; }
-keep class network.bisq.mobile.data.replicated.offer.price.spec.** { *; }

########################################
# androidx.datastore persistence
########################################

# Our persisted models + serializer wiring (the datastore library itself ships consumer rules)
-keep class network.bisq.mobile.data.model.** { *; }
-keep class network.bisq.mobile.data.datastore.** { *; }
-keep class * implements androidx.datastore.core.okio.OkioSerializer { *; }
-keepclassmembers class * implements androidx.datastore.core.okio.OkioSerializer {
    public <methods>;
}

########################################
# Tor
########################################

# NOTE: a keep for our own Tor service classes was removed — it pointed at a package deleted in
# the data-layer restructure (network.bisq.mobile.domain.service.network), and the live classes
# (data.service.network) survive shrink+optimize via direct references alone (dex-verified #1695).

# kmp-tor (runtime + resource loaders + controller): shrinking it breaks Tor bootstrap in
# release. Kept wholesale deliberately — security-critical infra, same policy as the node app.
# TODO evaluate narrowing (together with the node's identical rule).
-keep class io.matthewnelson.** { *; }

########################################
# General
########################################

# Keep any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin/annotation metadata (KotlinMetadata preserved via attributes, NOT a kotlin.Metadata
# class keep) + line numbers for readable production stack traces (we don't obfuscate).
-keepattributes KotlinMetadata
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Enums accessed via values()/valueOf reflection (settings, replicated enums over JSON)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

## Ktor's IntellijIdeaDebugDetector probes for IDE debugger attachment via
## java.lang.management.* — JVM-only APIs unavailable on Android. Safe to suppress;
## the detector returns false at runtime and the references are never executed.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

## Tink (com.google.crypto.tink) — used transitively for EncryptedSharedPreferences /
## push-notification-key encryption. Tink ships an unused KeysDownloader utility that
## references google-http-client + joda-time; those are not on our classpath because
## we never call KeysDownloader. R8 fails the build on the unresolved references unless
## we explicitly tell it to ignore them.
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn org.joda.time.Instant

########################################
# Log stripping (active: requires optimization, which is on above)
########################################

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class co.touchlab.kermit.Logger {
    public *** d(...);
    public *** v(...);
    public *** i(...);
}
