package network.bisq.gradle

import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.FilterConfiguration.FilterType
import com.android.build.gradle.AppPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Shared packaging rules for the two Bisq apps.
 *
 * Names the release artifacts from one product name:
 *  - APKs as `<Product_Name>-<versionName>-<abi>-<versionCode>.apk`
 *  - the AAB (through `archivesName`) as `<ProductName>-<versionName>_<versionCode>`
 *
 * gives every ABI split of one build its own version code, and decides when per-ABI APKs are built
 * at all (see [configureAbiSplits]).
 *
 * Naming replaces the legacy `applicationVariants.all { outputs.all { outputFileName = ... } }`
 * block, which AGP 9 removes along with `BaseVariantOutputImpl`. Public APIs only, so it compiles
 * the same on AGP 8.13 and 9.x. It needs two halves because the new `VariantOutput` carries a
 * version code but no file name: `onVariants` for the codes, an APK artifact transform for the
 * names.
 */
class AppArtifactsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("appArtifacts", AppArtifactsExtension::class.java)

        project.plugins.withType(AppPlugin::class.java) {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

            // archivesName is read when AGP creates the variants, so it has to be set before that.
            androidComponents.finalizeDsl { dsl ->
                val versionName = dsl.defaultConfig.versionName.orEmpty()
                val versionCode = abiVersionCode(dsl.defaultConfig.versionCode ?: 0, null)
                project.extensions.getByType(BasePluginExtension::class.java).archivesName.set(
                    "${extension.productName.get().replace(" ", "")}-${versionName}_$versionCode",
                )

                configureAbiSplits(project, dsl)
            }

            androidComponents.onVariants { variant ->
                val baseVersionCode =
                    variant.outputs
                        .firstOrNull()
                        ?.versionCode
                        ?.orNull
                if (baseVersionCode != null) {
                    variant.outputs.forEach { output ->
                        val abi = output.filters.firstOrNull { it.filterType == FilterType.ABI }?.identifier
                        output.versionCode.set(abiVersionCode(baseVersionCode, abi))
                    }
                }

                val suffix = variant.name.replaceFirstChar { it.uppercase() }
                val renameTask =
                    project.tasks.register<RenameApksTask>("renameApksFor$suffix") {
                        baseName.set(extension.productName.map { it.replace(" ", "_") })
                    }

                val transformationRequest =
                    variant.artifacts
                        .use(renameTask)
                        .wiredWithDirectories(RenameApksTask::apkFolder, RenameApksTask::renamedApkFolder)
                        .toTransformMany(SingleArtifact.APK)

                renameTask.configure { this.transformationRequest.set(transformationRequest) }
            }
        }
    }

    companion object {
        /**
         * Leaves three digits of room under each release for per-ABI codes. Bumping a release
         * therefore still outranks every ABI of the release before it.
         */
        private const val ABI_VERSION_CODE_MULTIPLIER = 1000

        /**
         * Ordinals are permanent: changing one rewrites the version code of an already published
         * ABI. 0 stays reserved for the universal APK so the store always prefers an ABI-specific
         * one when both match a device.
         */
        private val ABI_ORDINALS =
            mapOf(
                "armeabi-v7a" to 1,
                "arm64-v8a" to 2,
                "x86" to 3,
                "x86_64" to 4,
            )

        private fun abiVersionCode(
            baseVersionCode: Int,
            abi: String?,
        ): Int = baseVersionCode * ABI_VERSION_CODE_MULTIPLIER + (ABI_ORDINALS[abi] ?: 0)
    }
}

/**
 * Decides whether this build produces per-ABI APKs.
 *
 * Splits multiply packaging work, so they are only earned by the artifacts that ship:
 *  - `-PabiSplits=true|false` forces them on or off.
 *  - Otherwise they are on only when the build packages a non-debug APK, so a plain
 *    `assembleDebug` keeps the single universal APK it produced before this plugin existed.
 *  - `-Pabi=arm64-v8a[,x86_64]` narrows the set and drops the universal APK, for when one
 *    architecture is all you need. Naming an ABI enables splits by itself, so it works on a debug
 *    build too, unless `-PabiSplits=false` says otherwise.
 *
 * Bundles are a hard conflict rather than a preference: with resource shrinking on, asking for
 * splits and a bundle in the same build fails inside AGP with "Multiple shrunk-resources files
 * found" (https://issuetracker.google.com/402800800). Rather than silently dropping the per-ABI
 * APKs from a release run, that combination stops the build and says how to split it in two.
 */
private fun configureAbiSplits(
    project: Project,
    dsl: ApplicationExtension,
) {
    val abi = dsl.splits.abi
    if (!abi.isEnable) return

    val requestedAbis =
        project
            .findProperty(ABI_PROPERTY)
            ?.toString()
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    val unknown = requestedAbis - SUPPORTED_ABIS
    if (unknown.isNotEmpty()) {
        throw GradleException(
            "Unknown ABI(s) in -P$ABI_PROPERTY: ${unknown.joinToString()}. " +
                "Supported: ${SUPPORTED_ABIS.joinToString()}",
        )
    }

    val forced =
        project
            .findProperty(ABI_SPLITS_PROPERTY)
            ?.toString()
            ?.toBooleanStrictOrNull()
    // Naming an ABI is itself a request for per-ABI APKs, so it enables splits on its own rather
    // than being dropped on the floor by a debug build. An explicit -PabiSplits still wins.
    val wanted = forced ?: (requestedAbis.isNotEmpty() || project.packagesShippableApk())

    if (!wanted) {
        abi.isEnable = false
        return
    }

    val bundleTasks = project.requestedTasks().filter { it.startsWith("bundle", ignoreCase = true) }
    if (bundleTasks.isNotEmpty()) {
        throw GradleException(
            "ABI splits and app bundles cannot be built in one invocation (AGP fails with " +
                "\"Multiple shrunk-resources files found\", https://issuetracker.google.com/402800800).\n" +
                "Run them separately, for ${project.path}:\n" +
                "  ./gradlew ${project.path}:${bundleTasks.first()}\n" +
                "  ./gradlew ${project.path}:assembleRelease\n" +
                "or pass -P$ABI_SPLITS_PROPERTY=false to build both at once with a single universal APK.",
        )
    }

    if (requestedAbis.isEmpty()) return

    abi.reset()
    abi.include(*requestedAbis.toTypedArray())
    // One architecture was asked for, so the all-ABI fallback would just double the work.
    abi.isUniversalApk = false
    // Without a universal APK, AGP rejects any non-empty ndk abiFilters alongside split filters,
    // even an identical set (ApplicationVariantFactory.checkSplitsConflicts). The split include
    // list already restricts what is packaged, so hand that job over to it entirely.
    dsl.defaultConfig.ndk.abiFilters
        .clear()
}

/**
 * Requested task names that apply to this project: either unqualified, so every project runs them,
 * or explicitly addressed to this one. Without the path check, asking for
 * `:apps:nodeApp:bundleRelease` would also reconfigure clientApp and report errors against it.
 *
 * Splits are a DSL flag, decided long before the task graph exists, so the requested task names are
 * the only signal available. Gradle keys configuration cache entries on them, so branching here
 * stays cache-correct.
 */
private fun Project.requestedTasks(): List<String> =
    gradle.startParameter.taskNames.mapNotNull { requested ->
        val separator = requested.lastIndexOf(':')
        if (separator < 0) {
            requested
        } else {
            val target = requested.take(separator).let { if (it.startsWith(":")) it else ":$it" }
            requested.substring(separator + 1).takeIf { target == path }
        }
    }

/** True when the build packages an APK that ships, i.e. an assemble/install of a non-debug variant. */
private fun Project.packagesShippableApk(): Boolean =
    requestedTasks().any { name ->
        PACKAGING_TASK_PREFIXES.any { prefix -> name.startsWith(prefix, ignoreCase = true) } &&
            !name.contains("debug", ignoreCase = true) &&
            !name.contains("test", ignoreCase = true)
    }

private const val ABI_SPLITS_PROPERTY = "abiSplits"
private const val ABI_PROPERTY = "abi"
private val PACKAGING_TASK_PREFIXES = listOf("assemble", "package", "install")
private val SUPPORTED_ABIS = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

interface AppArtifactsExtension {
    /** Product name as written for humans, e.g. `Bisq Easy`. Spaces are stripped per artifact type. */
    val productName: Property<String>
}

abstract class RenameApksTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkFolder: DirectoryProperty

    @get:OutputDirectory
    abstract val renamedApkFolder: DirectoryProperty

    @get:Input
    abstract val baseName: Property<String>

    @get:Internal
    abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameApksTask>>

    @TaskAction
    fun rename() {
        val outputDir = renamedApkFolder.get().asFile
        val inputDir = apkFolder.get().asFile
        outputDir.mkdirs()
        // A version bump changes the file name, so the previous build's APKs would linger here.
        // Guarded: AGP is free to hand this task the directory it also reads from.
        if (outputDir.canonicalFile != inputDir.canonicalFile) {
            outputDir.listFiles { file -> file.extension == "apk" }?.forEach { it.delete() }
        }

        transformationRequest.get().submit(this) { builtArtifact ->
            val abi =
                builtArtifact.filters
                    .firstOrNull { it.filterType == FilterType.ABI }
                    ?.identifier
                    ?: UNIVERSAL_ABI
            val name =
                listOfNotNull(
                    baseName.get(),
                    builtArtifact.versionName?.takeIf { it.isNotBlank() },
                    abi,
                    builtArtifact.versionCode?.toString(),
                ).joinToString("-", postfix = ".apk")

            File(outputDir, name).also { target ->
                link(File(builtArtifact.outputFile), target)
            }
        }
    }

    /**
     * Hard link rather than copy. AGP has already written the packaged APK, and a release
     * universal APK is ~100 MB, so copying every output again doubles the packaging IO for no
     * gain. AGP replaces intermediates by rename rather than editing in place, so the link cannot
     * be mutated behind the artifact. Falls back to a copy where links are unavailable, such as a
     * build directory spanning filesystems or a filesystem without them.
     */
    private fun link(
        source: File,
        target: File,
    ) {
        target.delete()
        try {
            Files.createLink(target.toPath(), source.toPath())
        } catch (e: IOException) {
            logger.info("Hard link failed for ${target.name}, copying instead: ${e.message}")
            source.copyTo(target, overwrite = true)
        } catch (e: UnsupportedOperationException) {
            logger.info("Hard links unsupported here, copying ${target.name} instead: ${e.message}")
            source.copyTo(target, overwrite = true)
        }
    }

    private companion object {
        const val UNIVERSAL_ABI = "universal"
    }
}
