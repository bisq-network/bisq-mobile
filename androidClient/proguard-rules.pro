# Bisq Connect proguard file

## Ktor
### Keep Ktor Client Core
-keep class io.ktor.** { *; }
-keepattributes *Annotation*

### Avoid removing reflective access needed by Ktor's serialization
-keepnames class kotlinx.serialization.** { *; }

# Keep classes used by kmp persistance
-keep class network.bisq.mobile.domain.data.model.** { *; }

### The following were suggested by R8 engine, should be reviewed carefully on release build testing
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.gradle.api.Action
-dontwarn org.gradle.api.Named
-dontwarn org.gradle.api.Plugin
-dontwarn org.gradle.api.Task
-dontwarn org.gradle.api.artifacts.Dependency
-dontwarn org.gradle.api.artifacts.ExternalModuleDependency
-dontwarn org.gradle.api.attributes.Attribute
-dontwarn org.gradle.api.attributes.AttributeCompatibilityRule
-dontwarn org.gradle.api.attributes.AttributeContainer
-dontwarn org.gradle.api.attributes.AttributeDisambiguationRule
-dontwarn org.gradle.api.attributes.HasAttributes
-dontwarn org.gradle.api.component.SoftwareComponent
-dontwarn org.gradle.api.plugins.ExtensionAware
-dontwarn org.gradle.api.tasks.util.PatternFilterable

## General

# Keep Kotlin Metadata
-keepattributes KotlinMetadata

# Keep KMP Framework Class Names
-keep class kotlinx.** { *; }

# Avoid stripping enums used by KMP
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Compose Compiler Intrinsics
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }

# Keep Compose Preview Annotations (if using Android Studio Preview)
-keep @androidx.compose.ui.tooling.preview.Preview class * { *; }

# Keep Composer Intrinsics
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }

# Keep all classes annotated with @Composable
-keep class * {
    @androidx.compose.runtime.Composable *;
}

# Keep Compose compiler metadata
-keepattributes *Annotation*

# Keep Jetpack Compose runtime classes
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Koin classes and avoid stripping DI components
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <fields>;
    @org.koin.core.annotation.* <methods>;
}