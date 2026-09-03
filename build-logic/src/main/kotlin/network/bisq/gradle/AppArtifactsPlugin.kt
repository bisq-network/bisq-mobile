package network.bisq.gradle

import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.FilterConfiguration.FilterType
import com.android.build.gradle.AppPlugin
import org.gradle.api.DefaultTask
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

/**
 * Shared packaging rules for the two Bisq apps.
 *
 * Names the release artifacts from one product name:
 *  - APKs as `<Product_Name>-<versionName>-<abi>-<versionCode>.apk`
 *  - the AAB (through `archivesName`) as `<ProductName>-<versionName>_<versionCode>`
 *
 * gives every ABI split of one build its own version code, and turns ABI splits off for bundle
 * builds, which cannot be produced while they are on.
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

                // An AAB already carries every ABI and Play splits it server side, so per-ABI APKs
                // are redundant there. AGP does not merely ignore them: with resource shrinking on,
                // buildPreBundle fails with "Multiple shrunk-resources files found"
                // (https://issuetracker.google.com/402800800). Requesting a bundle therefore drops
                // the splits rather than the shrinker.
                if (dsl.splits.abi.isEnable && project.isBundleRequested()) {
                    dsl.splits.abi.isEnable = false
                    project.logger.lifecycle(
                        "ABI splits disabled for ${project.path}: incompatible with bundle tasks.",
                    )
                }
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

// Splits are a DSL flag, decided long before the task graph exists, so the requested task names
// are the only signal available. Gradle keys configuration cache entries on them, so branching
// here stays cache-correct.
private fun Project.isBundleRequested(): Boolean =
    gradle.startParameter.taskNames.any {
        it.substringAfterLast(':').startsWith("bundle", ignoreCase = true)
    }

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
                File(builtArtifact.outputFile).copyTo(target, overwrite = true)
            }
        }
    }

    private companion object {
        const val UNIVERSAL_ABI = "universal"
    }
}
