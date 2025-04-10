plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
                // Add webpack fallbacks for Node.js core modules
                devServer = devServer?.copy(
                    open = false,
                    port = 8080
                )
                // Configure webpack fallbacks for Node.js core modules
//                this won't work
//                commonWebpackConfig {
//                    resolve {
//                        fallback = mapOf(
//                            "os" to false,
//                            "path" to false,
//                            "fs" to false,
//                            "crypto" to false,
//                            "buffer" to false,
//                            "stream" to false
//                        )
//                    }
//                }
            }
            runTask {
                mainOutputFileName = "main.js"
                devServer = devServer.let {
                    it.copy(
                        open = false,
                        port = 8080
                    )
                }
            }
            webpackTask {
                mainOutputFileName = "main.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        
        binaries.executable()
    }
    
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared:domain"))
                implementation(project(":shared:presentation"))
                implementation(compose.web.core)
                implementation(compose.runtime)
                
                // Add Koin for dependency injection
                implementation(libs.koin.core)
                
                // Add other necessary dependencies
                implementation(libs.kotlinx.coroutines.core.js)
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.websockets)
                
                // Add multiplatform settings
                implementation(libs.multiplatform.settings)
                
                // Add Kermit for logging
                implementation(libs.logging.kermit)

                // Add Node.js polyfills
                implementation(npm("os-browserify", "0.3.0"))
                implementation(npm("path-browserify", "1.0.1"))
                implementation(npm("buffer", "6.0.3"))
                implementation(npm("stream-browserify", "3.0.0"))
                
                // Add lifecycle dependencies
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.navigation.compose)
            }
        }
    }
}

// not needed anymore
//compose.experimental {
//    web.application {}
//}