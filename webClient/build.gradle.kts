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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.7.3")
                implementation("io.ktor:ktor-client-js:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
                implementation("io.ktor:ktor-client-websockets:2.3.5")
            }
        }
    }
}

compose.experimental {
    web.application {}
}