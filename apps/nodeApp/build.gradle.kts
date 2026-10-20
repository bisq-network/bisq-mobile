import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.kover)
    id("network.bisq.mobile.app-artifacts")
}

// -------------------- Version Configuration --------------------
version = project.findProperty("node.android.version") as String
val versionCodeValue = (project.findProperty("node.android.version.code") as String).toInt()

// Empty for the build types which are not obfuscated; the release variant gets its UUID at the end
// of this file
val sentryProguardUuidPlaceholder = "sentryProguardUuid"
val sharedVersion = project.findProperty("shared.version") as String
val appName = project.findProperty("node.name") as String

// -------------------- Module References --------------------
val sharedPresentationModule = ":shared:presentation"
val sharedDomainModule = ":shared:domain"
val sharedTestUtilsModule = ":shared:test-utils"

// -------------------- Kotlin Multiplatform Configuration --------------------
kotlin {
    // using JDK21 for full bisq2 compatibility
    jvmToolchain(21)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Project modules
            api(project(sharedPresentationModule))

            // Compose
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui.tooling.preview)

            // Other libraries
            implementation(libs.navigation.compose)
        }

        androidMain.dependencies {
            // AndroidX
            implementation(libs.androidx.activity.compose)
        }

        androidUnitTest.dependencies {
            // Kotlin
            implementation(libs.kotlin.test.junit)
            implementation(libs.kotlinx.coroutines.test)

            // Compose UI test
            implementation(libs.androidx.test.compose.junit4)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.compose.manifest)
            implementation(libs.compose.ui.test)

            // Other libraries
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.robolectric)

            // Test utilities
            implementation(project(sharedTestUtilsModule))
        }
    }
}

// -------------------- Local Properties / release signing --------------------
apply(from = rootProject.file("gradle/releaseSigning.gradle.kts"))
val localProperties: Properties = extra["localProperties"] as Properties
val releaseKeystoreFile: File? = extra["releaseKeystoreFile"] as File?

@Suppress("UNCHECKED_CAST")
val optionalSigningProp = extra["optionalSigningProp"] as (String) -> String
extra["requiredCompanionProps"] = listOf("KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")

// -------------------- App artifact naming / packaging --------------------
appArtifacts {
    productName.set(appName)
}

// -------------------- Android Configuration --------------------
android {
    namespace = "network.bisq.mobile.node"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    // pin ndk version for deterministic builds
    ndkVersion =
        libs.versions.android.ndk
            .get()

    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = optionalSigningProp("KEYSTORE_PASSWORD")
                keyAlias = optionalSigningProp("KEY_ALIAS")
                keyPassword = optionalSigningProp("KEY_PASSWORD")
            }
        }
    }

    sourceSets {
        getByName("debug") {
            java {
                srcDir("src/main/resources")
            }
        }
        getByName("release") {
            java {
                srcDir("src/release/resources")
            }
        }
    }

    defaultConfig {
        applicationId = "network.bisq.mobile.node"
        minSdk =
            libs.versions.android.node.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        multiDexEnabled = true
        versionCode = versionCodeValue
        versionName = project.version.toString()
        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")

        // bisq2 core root log level, only honored in debug builds (release/profile silence the
        // core - see Bisq2LoggingSetup). Default in gradle.properties, per-dev override via
        // BISQ2_LOG_LEVEL in local.properties. Defined in defaultConfig because the consuming
        // code compiles in every variant.
        val bisq2LogLevel =
            localProperties.getProperty("BISQ2_LOG_LEVEL")
                ?: (project.findProperty("node.bisq2.log.level") as? String ?: "INFO")
        buildConfigField("String", "BISQ2_LOG_LEVEL", "\"$bisq2LogLevel\"")

        // Memory management configuration
        // Default: extended heap. Turn false to test for mem leaks reducing heap size.
        manifestPlaceholders["largeHeap"] = "true"
        manifestPlaceholders[sentryProguardUuidPlaceholder] = ""

        // ABI filters for APK release build after Tor inclusion
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    // ABI splits: one APK per architecture instead of one carrying all four copies of
    // libtor.so. The universal APK stays for sideloading, and AppArtifactsPlugin gives each output
    // its own version code. That plugin also switches this off for bundle builds, which AGP
    // cannot produce while splits are on.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude conflicting META-INF files to avoid protobuf build issues
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE*.md")
            excludes.add("META-INF/NOTICE*.md")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/NOTICE.markdown")

            // bisq2's JVM tor stack contributes a desktop tor bundle as a jar resource. It only
            // materializes when the build host is macOS, which is how the published APKs are
            // produced: Bisq_Easy-0.12.0.apk carries an 18.6 MB tor.zip of Mach-O binaries
            // (tor, libevent dylib, lyrebird, conjure-client) that cannot execute on Android.
            // Android drives tor through kmp-tor's lib/<abi>/libtor.so instead, so drop it.
            excludes.add("tor.zip")

            // grpc-netty-shaded ships tcnative and epoll binaries as jar resources for every
            // desktop OS: Windows .dll, macOS .jnilib and glibc-linked Linux .so. None can be
            // loaded on Android. AGP's java-resource merger already drops the .so on its own, so
            // only the .dll and .jnilib (9.6 MB) actually reached the APK, but excluding the whole
            // directory covers all of them and does not rot if that merger behaviour changes.
            // Nothing under META-INF/native/ is loadable here: Android takes native code from
            // lib/<abi>/ only, so netty stays on the JDK TLS stack and NIO transport either way.
            excludes.add("META-INF/native/**")

            pickFirsts.add("**/protobuf/**/*.class")
            pickFirsts +=
                listOf(
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/services/**",
                    "META-INF/*.version",
                )
        }
        jniLibs {
            // Pick first for duplicate native libraries across dependencies
            pickFirsts +=
                listOf(
                    "lib/**/libtor.so",
                    "lib/**/libcrypto.so",
                    "lib/**/libevent*.so",
                    "lib/**/libssl.so",
                    "lib/**/libsqlite*.so",
                    "lib/**/libdatastore_shared_counter.so",
                )
            // Exclude problematic native libraries
            excludes +=
                listOf(
                    "**/libmagtsync.so",
                    "**/libMEOW*.so",
                )
            // Required for kmp-tor exec resources - helps prevent EOCD corruption
            useLegacyPackaging = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            // General full shrinking brings issues with protobuf in jars
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
            isDebuggable = false
            isCrunchPngs = true
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Reduce GC logging noise in debug builds
            buildConfigField("String", "GC_LOG_LEVEL", "\"WARN\"")

            // Disable minification in debug to avoid lock verification issues
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("profile") {
            initWith(getByName("release"))
            if (releaseKeystoreFile == null) {
                signingConfig = signingConfigs.getByName("debug")
            }
            // Make debuggable so Android Studio can attach allocation tracking on all devices
            isDebuggable = true
            // Easier symbol readability in profiler; flip to true to mimic release exactly
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".profile"
            versionNameSuffix = "-profile"
            matchingFallbacks += listOf("release")
        }
    }
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        // For bisq2 jars full compatibility
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// -------------------- Protobuf Configuration --------------------
// Compatible with macOS on Apple Silicon
val archSuffix = if (Os.isFamily(Os.FAMILY_MAC)) ":osx-x86_64" else ""

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protob.get()}$archSuffix"
    }
    generateProtoTasks {
        all().forEach { task ->
            val variantName =
                Regex("(debug|release|profile)", RegexOption.IGNORE_CASE)
                    .find(task.name)
                    ?.value
                    ?.lowercase() ?: "debug"
            task.inputs.dir(layout.buildDirectory.dir("/extracted-include-protos/$variantName"))
            task.builtins {
                create("java")
            }
        }
    }
}

// -------------------- Dependencies --------------------
val bisq2JitPackGroup =
    libs.bisq.core.common
        .get()
        .module.group

// bisq2 declares every library it uses with a strict version, the one it is built and tested
// against, and the jars of a local publishAll keep them. JitPack rewrites the metadata of its jars
// and turns them into plain versions, which any higher version elsewhere in the graph silently
// overrides, and verifyBisqCoreDependencyVersions below would no longer see. Restore them.
@CacheableRule
abstract class StrictBisq2Dependencies
    @Inject
    constructor(
        private val bisq2Group: String,
    ) : ComponentMetadataRule {
        override fun execute(context: ComponentMetadataContext) {
            if (context.details.id.group != bisq2Group) return
            context.details.allVariants {
                withDependencies {
                    filter { it.group != bisq2Group }.forEach { dependency ->
                        val version = dependency.versionConstraint.requiredVersion
                        if (version.isNotEmpty()) dependency.version { strictly(version) }
                    }
                }
            }
        }
    }
dependencies.components.all(StrictBisq2Dependencies::class.java) { params(bisq2JitPackGroup) }

// BISQ2_FROM_MAVEN_LOCAL (see settings.gradle.kts) builds against the bisq2 jars of a local
// `./gradlew publishAll` instead of JitPack's. Those keep bisq2's own groups and release version,
// and their metadata names the other bisq2 modules the same way, so only the modules declared
// below need substituting.
val bisq2FromMavenLocal = gradle.extra["bisq2FromMavenLocal"] as Boolean
// A line in local.properties decides which bisq2 the node app embeds, so say which one it is
// whenever the build is configured, which includes the first build after that line changes
logger.lifecycle(
    if (bisq2FromMavenLocal) {
        "bisq2: ~/.m2 ${libs.versions.bisq.core.asProvider().get()} (BISQ2_FROM_MAVEN_LOCAL in local.properties)"
    } else {
        "bisq2: JitPack ${libs.versions.bisq.core.commit.get()}"
    },
)
if (bisq2FromMavenLocal) {
    val bisqCoreVersion =
        libs.versions.bisq.core
            .asProvider()
            .get()
    // bisq2 publishes its network modules, which the catalog names bisq-core-network-*, under its
    // network group and every other module the node app declares under bisq. Older publishAll runs
    // also left jar-only aliases of the network modules under bisq, so the mapping must be exact.
    val bisq2NetworkModules =
        versionCatalogs.named("libs").let { catalog ->
            catalog.libraryAliases
                .filter { it.startsWith("bisq.core.network.") }
                .map {
                    catalog
                        .findLibrary(it)
                        .get()
                        .get()
                        .module.name
                }.toSet()
        }
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            all {
                val requested = requested as? ModuleComponentSelector ?: return@all
                if (requested.group != bisq2JitPackGroup) return@all
                val localGroup = if (requested.module in bisq2NetworkModules) "network" else "bisq"
                useTarget("$localGroup:${requested.module}:$bisqCoreVersion", "BISQ2_FROM_MAVEN_LOCAL is set")
            }
        }
    }
}

// Exclude conflicting jsocks fork from bisq-network. The canonical jsocks classes are
// provided transitively via bisq2's own jsocks module (pulled in by bisq.core.network.network),
// which JitPack serves under com.github.bisq-network.bisq2, a group this rule does not match.
configurations.all {
    exclude(group = "com.github.bisq-network", module = "jsocks")
}

// net.i2p:router 2.12.0 added post-quantum (MLKEM) support and ships a shaded copy of 60
// org/bouncycastle/* classes inside its jar. Mobile pulls clean bcprov-jdk18on directly,
// and Android's R8 fails the build on the duplicate classes. Mobile only enables TOR
// transport at runtime (see android.conf: supportedTransportTypes = ["TOR"]), so the
// shaded BC classes are dead code on this platform. Strip them at consume time via a
// Gradle artifact transform; bisq2 desktop avoids this because the JVM tolerates
// duplicate classpath classes (first-found wins) where R8 doesn't.
abstract class StripShadedBouncyCastle : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        // Pass through everything except net.i2p:router. Use the Provider-based outputs.file
        // overload so Gradle wires the input file as the output without copying or relocating.
        if (!input.name.startsWith("router-")) {
            outputs.file(inputArtifact)
            return
        }
        val output = outputs.file(input.nameWithoutExtension + "-no-bc.jar")
        JarOutputStream(output.outputStream().buffered()).use { jos ->
            JarFile(input).use { jf ->
                jf
                    .entries()
                    .asSequence()
                    .filterNot { it.name.startsWith("org/bouncycastle/") }
                    .forEach { entry ->
                        jos.putNextEntry(JarEntry(entry.name))
                        if (!entry.isDirectory) {
                            jf.getInputStream(entry).use { it.copyTo(jos) }
                        }
                        jos.closeEntry()
                    }
            }
        }
    }
}

val stripShadedBcAttribute = Attribute.of("stripShadedBc", Boolean::class.javaObjectType)

// Producers that don't declare the attribute (e.g. project-local artifacts from Android/Kotlin
// compile tasks) must remain compatible with consumers that request stripShadedBc = true.
// Without this rule, requesting the attribute on a classpath silently excludes project artifacts
// and tests fail with NoClassDefFoundError on the project's own classes.
class StripShadedBcCompatibility : AttributeCompatibilityRule<Boolean> {
    override fun execute(details: CompatibilityCheckDetails<Boolean>) {
        if (details.producerValue == null || details.producerValue == details.consumerValue) {
            details.compatible()
        }
        // Else (producer=false, consumer=true) leave unset so Gradle picks the registered transform.
    }
}

// Only the APK runtime/compile classpaths need the transform — that's where R8/D8 enforces
// no-duplicate-classes. JVM unit tests tolerate duplicate classes (first-found wins), and
// stamping the attribute on test classpaths excludes project-local artifacts even with a
// compatibility rule in place (NoClassDefFoundError on the project's own Kotlin output).
val apkClasspathNames =
    setOf(
        "debugCompileClasspath",
        "debugRuntimeClasspath",
        "releaseCompileClasspath",
        "releaseRuntimeClasspath",
        "profileCompileClasspath",
        "profileRuntimeClasspath",
    )
configurations.matching { it.isCanBeResolved && it.name in apkClasspathNames }.configureEach {
    attributes.attribute(stripShadedBcAttribute, true)
}

// Force Bisq2 core dependency versions in unit tests to match production
// Robolectric brings its own bcprov-jdk18on version, but the tests must run on the one of Bisq2
configurations.matching { it.name.contains("UnitTest") }.configureEach {
    resolutionStrategy {
        force("${libs.bouncycastle.prov.get().module}:${libs.versions.bouncycastle.lib.get()}")
        force("${libs.bouncycastle.pg.get().module}:${libs.versions.bouncycastle.lib.get()}")
        force("${libs.google.guava.get().module}:${libs.versions.google.guava.lib.get()}")
    }
}

// The bisq2 jars request the versions of their libraries strictly: the core is built and tested
// against exactly those. A strict version declared by this app for the same library wins over
// them without a word, and the core then runs on a library it was never tested with. This fails
// the build instead. On failure, set the version in gradle/libs.versions.toml to the one named.
abstract class VerifyBisqCoreDependencyVersionsTask : DefaultTask() {
    @get:Input
    abstract val rootComponent: Property<ResolvedComponentResult>

    @TaskAction
    fun verify() {
        val visited = mutableSetOf<ResolvedComponentResult>()
        val pending = ArrayDeque(listOf(rootComponent.get()))
        val mismatches = sortedSetOf<String>()
        while (pending.isNotEmpty()) {
            val component = pending.removeFirst()
            if (!visited.add(component)) continue
            val requester = component.id as? ModuleComponentIdentifier
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
                pending.add(dependency.selected)
                val requested = dependency.requested as? ModuleComponentSelector ?: return@forEach
                val strictVersion = requested.versionConstraint.strictVersion
                val selectedVersion = dependency.selected.moduleVersion?.version
                if (requester?.group in BISQ_CORE_GROUPS && strictVersion.isNotEmpty() && strictVersion != selectedVersion) {
                    mismatches.add("${requested.module}: bisq2 requests $strictVersion, the app resolves $selectedVersion")
                }
            }
        }
        if (mismatches.isNotEmpty()) {
            throw GradleException(
                "Libraries of bisq2 core resolve to another version than the one its jars request:\n" +
                    mismatches.joinToString("\n") { "  - $it" } +
                    "\nAlign the versions in gradle/libs.versions.toml (Bisq Core Transitive Dependencies).",
            )
        }
    }

    private companion object {
        // The group JitPack serves bisq2 under, and the groups of a local publishAll
        val BISQ_CORE_GROUPS = setOf("com.github.bisq-network.bisq2", "bisq", "network", "tor")
    }
}

val verifyBisqCoreDependencyVersions =
    tasks.register<VerifyBisqCoreDependencyVersionsTask>("verifyBisqCoreDependencyVersions") {
        group = "verification"
        description = "Fails when a library of bisq2 core resolves to another version than its jars request"
        // The classpath of the app which ships
        rootComponent.set(configurations.named("releaseRuntimeClasspath").flatMap { it.incoming.resolutionResult.rootComponent })
    }
tasks.named("preBuild") { dependsOn(verifyBisqCoreDependencyVersions) }

dependencies {
    // Register the BC-strip artifact transform (definition + producer/consumer attribute setup)
    attributesSchema {
        attribute(stripShadedBcAttribute) {
            compatibilityRules.add(StripShadedBcCompatibility::class.java)
        }
    }
    artifactTypes.getByName("jar") {
        attributes.attribute(stripShadedBcAttribute, false)
    }
    registerTransform(StripShadedBouncyCastle::class.java) {
        from.attribute(stripShadedBcAttribute, false).attribute(ARTIFACT_TYPE_ATTRIBUTE, "jar")
        to.attribute(stripShadedBcAttribute, true).attribute(ARTIFACT_TYPE_ATTRIBUTE, "jar")
    }

    // Project modules
    implementation(project(sharedPresentationModule))
    implementation(project(sharedDomainModule))

    // Debug tools
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.test.compose.manifest)

    // Android libraries
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.core.splashscreen)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Bisq2 core dependencies
    implementation(libs.google.guava)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.typesafe.config)

    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pg)

    // Bisq2 core modules
    implementation(libs.bisq.core.common)
    implementation(libs.bisq.core.i18n)
    implementation(libs.bisq.core.persistence)
    implementation(libs.bisq.core.security)
    implementation(libs.bisq.core.identity)
    implementation(libs.bisq.core.account)
    implementation(libs.bisq.core.burningman)
    implementation(libs.bisq.core.settings)
    implementation(libs.bisq.core.bonded.roles)
    implementation(libs.bisq.core.user)
    implementation(libs.bisq.core.contract)
    implementation(libs.bisq.core.offer)
    implementation(libs.bisq.core.trade)
    implementation(libs.bisq.core.support)
    implementation(libs.bisq.core.application)
    implementation(libs.bisq.core.chat)
    implementation(libs.bisq.core.presentation)
    implementation(libs.bisq.core.bisq.easy)
    implementation(libs.bisq.core.notifications)

    // Bisq2 network modules
    implementation(libs.bisq.core.network.network)
    implementation(libs.bisq.core.network.network.identity)
    implementation(libs.bisq.core.network.socks5.socket.channel)
    implementation(libs.bisq.core.network.i2p)
    // jsocks classes are provided transitively by bisq.core.network.network (which depends
    // on bisq2's jsocks module). Adding com.github.chimp1984:jsocks would duplicate them.
    implementation(libs.failsafe)

    // Protobuf
    implementation(libs.protoc)

    // Dependency injection & logging
    implementation(libs.koin.compose)
    implementation(libs.koin.android)
    implementation(libs.logging.kermit)
    // Compile-classpath access to the logback API the bisq2 jars already ship at runtime
    // (their POMs publish it runtime-scoped), for Bisq2LogcatAppender / Bisq2LoggingSetup.
    implementation(libs.logging.logback.classic)
}

// -------------------- Build Tasks Configuration --------------------
// Ensure tests run on the same Java version as the main code
tasks.withType<Test> {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}

// Ensure generateResourceBundles runs before Android build tasks
afterEvaluate {
    val generateResourceBundlesTask =
        project(sharedDomainModule).tasks.findByName("generateResourceBundles")
    if (generateResourceBundlesTask != null) {
        tasks
            .matching { task ->
                task.name.startsWith("compile") ||
                    task.name.startsWith("assemble") ||
                    task.name.startsWith("bundle") ||
                    task.name.contains("Build")
            }.configureEach {
                dependsOn(generateResourceBundlesTask)
            }
    }
}

// -------------------- ProGuard Mapping Configuration --------------------
extra["moduleName"] = "nodeApp"
extra["mappingFlavors"] = emptyList<String>()
extra["mappingVersionCode"] = versionCodeValue
apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")

// The release app carries the UUID under which its ProGuard mapping is uploaded, so an obfuscated
// stack trace can be matched to that mapping. See gradle/mapping-tasks.gradle.kts.
@Suppress("UNCHECKED_CAST")
val proguardUuidOfFlavor = extra["proguardUuidOfFlavor"] as (String?) -> String
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val flavor = variant.flavorName?.takeIf { it.isNotEmpty() }
        variant.manifestPlaceholders.put(sentryProguardUuidPlaceholder, proguardUuidOfFlavor(flavor))
    }
}
