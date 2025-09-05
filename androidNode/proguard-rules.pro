# Bisq Node proguard file

### protobuf deps
-dontwarn java.lang.MatchException

# Keep gRPC and Netty classes
-keep class io.grpc.** { *; }
-keep class io.netty.** { *; }
-keep class io.grpc.netty.shaded.io.netty.** { *; }

# Keep all the missing classes that are referenced but not directly used
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.nano.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.core.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.lz4.**
-dontwarn net.jpountz.xxhash.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.bouncycastle.openssl.**
-dontwarn org.bouncycastle.operator.**
-dontwarn org.bouncycastle.pkcs.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.jboss.marshalling.**
-dontwarn reactor.blockhound.**
-dontwarn sun.security.x509.**

# Don't shrink/obfuscate build-time plugins
-dontwarn com.android.build.**
-dontwarn com.google.protobuf.gradle.**
-dontwarn org.codehaus.groovy.**
-dontwarn javax.inject.**
-dontwarn org.gradle.**
-dontwarn javassist.**
-dontwarn org.apache.maven.**
-dontwarn kr.motd.maven.**
-dontwarn org.eclipse.**

# Keep core Android/Gradle plugin APIs
-keep class com.android.** { *; }
-keep class org.gradle.** { *; }

# Keep any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all classes that might be used via reflection
-keep class * implements java.io.Serializable { *; }

# Keep classes used by androidx.datastore persistence
-keep class network.bisq.mobile.domain.data.model.** { *; }
-keep class network.bisq.mobile.domain.data.datastore.** { *; }

# Keep androidx.datastore classes and serializers
-keep class androidx.datastore.** { *; }
-keep class * implements androidx.datastore.core.okio.OkioSerializer { *; }
-keepclassmembers class * implements androidx.datastore.core.okio.OkioSerializer {
    public <methods>;
}

###########################################
# Core Bisq Protobuf preservation rules
###########################################

# Keep all Bisq core classes
-keep class org.bisq.** { *; }
-keep class bisq.** { *; }
-keep class chat.** { *; }
-keep class network.** { *; }
-keep class bonded_roles.** { *; }
-keep class user.** { *; }

# Keep all Protobuf-related classes
-keep class com.google.protobuf.** { *; }

# Keep names for Protobuf types to match type_url
-keepnames class * implements com.google.protobuf.MessageLite
-keepnames class * extends com.google.protobuf.GeneratedMessageLite
-keepnames class * extends com.google.protobuf.GeneratedMessageV3

# Keep inner builder classes and their members
-keep class **$Builder { *; }
-keepclassmembers class *$Builder { *; }

# Keep class members of any class extending GeneratedMessageLite
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# Keep all protobuf resolver classes and their names - CRITICAL FOR PROTOBUF DESERIALIZATION
-keep class bisq.common.proto.ProtoResolver { *; }
-keep class bisq.common.proto.PersistableProtoResolverMap { *; }
-keep class bisq.common.proto.NetworkStorageWhiteList { *; }
-keep class * implements bisq.common.proto.PersistableProtoResolver { *; }

# Keep all static initializers (needed for PersistableProtoResolverMap)
-keepclassmembers class * {
    static <clinit>();
}

# Keep annotations used for proto type resolving
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes *Annotation*

# Keep fields and methods used for reflection
-keepclassmembers class * {
    @com.google.protobuf.* *;
}

# Keep resolver and registration methods
-keepclassmembers class * {
    static *** register*(...);
    public static void register(...);
    public static * fromAny(...);
    public static bisq.common.proto.ProtoResolver getResolver();
    public static bisq.common.proto.ProtoResolver getNetworkMessageResolver();
    public static * fromProto(*);
}

# Keep protobuf internal cached size fields and related synthetic methods
-keepclassmembers class * {
    int memoizedSerializedSize;
    int memoizedSize;
    int memoizedHashCode;
    synthetic <methods>;
}
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    public int getSerializedSize();
}

# Keep statics and enums used in Protobuf
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * {
    static <fields>;
    static <methods>;
}

# Keep anything under .proto. packages if they exist
-keep class **.proto.** { *; }
-keepnames class **.proto.**

# Keep persistence store classes
-keep class bisq.persistence.** { *; }
-keep class bisq.network.identity.** { *; }

# Keep all Bouncy Castle classes
-keep class org.bouncycastle.** { *; }

# Keep all Tor-related classes
-keep class org.torproject.** { *; }

# Ignore missing Java desktop/server classes
-dontwarn com.sun.net.httpserver.**
-dontwarn jakarta.servlet.**
-dontwarn java.awt.**
-dontwarn java.awt.image.**
-dontwarn javax.servlet.**
-dontwarn javax.**
-dontwarn jakarta.**

# Keep Logback and SLF4J classes
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**

# Keep Bisq logging classes specifically
-keep class bisq.common.logging.** { *; }

# Keep logback configuration classes that use reflection
-keep class ch.qos.logback.core.rolling.** { *; }
-keep class ch.qos.logback.classic.** { *; }

## General Android/Kotlin/Compose

# Keep Kotlin Metadata
-keepattributes KotlinMetadata
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep KMP Framework Class Names
-keep class kotlinx.** { *; }

# Keep Compose Compiler Intrinsics - More specific rules to avoid lock verification issues
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.snapshots.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep all classes annotated with @Composable
-keep class * {
    @androidx.compose.runtime.Composable *;
}

# Prevent lock verification issues with Compose state management
-keep class androidx.compose.runtime.snapshots.SnapshotStateList {
    public <methods>;
}

# Keep Compose compiler generated classes
-keep class **.*ComposableSingletons* { *; }
-keep class **.*LiveLiterals* { *; }

# Keep Composer Intrinsics
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }

# Keep Compose Preview Annotations
-keep @androidx.compose.ui.tooling.preview.Preview class * { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Koin classes and avoid stripping DI components
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <fields>;
    @org.koin.core.annotation.* <methods>;
}

# Comprehensive -dontwarn section (consolidated)
-dontwarn com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
-dontwarn com.sun.xml.fastinfoset.sax.AttributesHolder
-dontwarn com.sun.jdi.**
-dontwarn com.sun.xml.fastinfoset.stax.**
-dontwarn groovy.lang.**
-dontwarn groovy.transform.Generated
-dontwarn java.beans.**
-dontwarn java.lang.Module
-dontwarn java.lang.management.**
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.imageio.**
-dontwarn javax.mail.**
-dontwarn javax.management.**
-dontwarn javax.naming.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.avalon.framework.logger.Logger
-dontwarn org.apache.log.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.commons.logging.impl.Log4JLogger
-dontwarn org.apache.maven.**
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.codehaus.groovy.**
-dontwarn org.codehaus.janino.**
-dontwarn org.codehaus.plexus.component.annotations.Component
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.eclipse.ui.IStartup
-dontwarn org.graalvm.nativeimage.hosted.Feature
-dontwarn org.hibernate.validator.HibernateValidator
-dontwarn org.ietf.jgss.**
-dontwarn org.jvnet.**
-dontwarn org.osgi.framework.**
-dontwarn sun.reflect.Reflection

# Keep specific classes that need explicit preservation
-keep class org.apache.commons.logging.impl.Log4JLogger { *; }

# Disable all optimizations that could break protobuf
-dontoptimize
-dontobfuscate

# Keep all protobuf and resolver classes completely intact
-keep class bisq.common.proto.** { *; }
-keep class bisq.persistence.** { *; }
-keep class bisq.network.p2p.services.data.storage.** { *; }
-keep class bisq.network.p2p.message.** { *; }

# Keep all protobuf generated classes
-keep class com.google.protobuf.** { *; }
-keep class **.protobuf.** { *; }

# Keep all store classes and their methods
-keep class **.*Store { *; }
-keep class * implements bisq.persistence.PersistableStore { *; }

# Keep resolver registration
-keep class bisq.application.ResolverConfig { *; }

# Keep all lambda expressions and synthetic methods
-keep class * {
    synthetic <methods>;
    static synthetic <methods>;
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# More aggressive external library shrinking
-keep class !bisq.**,!network.bisq.**,!com.google.protobuf.**,!io.grpc.**,!io.netty.**,!org.bouncycastle.**,!ch.qos.logback.**,!org.slf4j.** { *; }

# Allow removal of unused external library methods and debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove Kermit debug/info/verbose logs in release builds
-assumenosideeffects class co.touchlab.kermit.Logger {
    public *** d(...);
    public *** v(...);
    public *** i(...);
}

# Remove debug log calls from our logging interface
-assumenosideeffects class * implements network.bisq.mobile.domain.utils.Logging {
    *** log.d(...);
    *** log.v(...);
    *** log.i(...);
}

# Remove System.out and System.err calls from Bisq2 JARs in release builds
-assumenosideeffects class java.lang.System {
    public static java.io.PrintStream out;
    public static java.io.PrintStream err;
}
-assumenosideeffects class java.io.PrintStream {
    public *** println(...);
    public *** print(...);
    public *** printf(...);
    public *** format(...);
}

# Remove specific verbose logging calls from Bisq2 protobuf classes
-assumenosideeffects class bisq.network.protobuf.** {
    *** getSerializedSize(...);
}
-assumenosideeffects class bisq.chat.protobuf.** {
    *** getSerializedSize(...);
}
-assumenosideeffects class bisq.offer.protobuf.** {
    *** getSerializedSize(...);
}
