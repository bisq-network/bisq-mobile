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
            }
            runTask {
                outputFileName = "main.js"
                devServer = devServer.let {
                    it.copy(
                        open = false,
                        port = 8080
                    )
                }
            }
            webpackTask {
                outputFileName = "main.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        // TODO specific node.js config?
        
        binaries.executable()
    }
    
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared:domain"))
                implementation(project(":shared:presentation"))
                implementation(compose.web.core)
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core.js)
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.websockets)
            }
        }
    }
}

compose.experimental {
    web.application {}
}