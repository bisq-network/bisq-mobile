plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.protobuf")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.4"
    }

    generateProtoTasks {
        all().forEach { task ->
            // Set the source of the .proto files to the "src/main/proto" directory
            //task.source("src/main/proto")

            // Use standard Java classes (not lite) for Protobuf generation
            task.builtins {
                java {
                   // outputSubDir = "java"  // Specify output directory for Java files
                }  // Generates standard Protobuf Java classes
            }
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Generate standard (non-lite) Java classes
                create("java")
            }
        }
    }
}

android {
    namespace = "bisq.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "bisq.mobile"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/resources")
            java.srcDir("src/main/proto")
            java.srcDir("build/generated/source/proto/main/java")
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude the conflicting META-INF files
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            excludes.add("META-INF/DEPENDENCIES")

//            pickFirsts.add("bisq/account/protobuf/Account\$Builder.class")
            pickFirsts.add("**/protobuf/**/*.class")
        }
    }
}

//configurations {
//    all {
//        exclude(group = "com.google.protobuf")
//    }
//}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation("bisq:common:2.1.2")
    implementation(libs.typesafe.config)
    implementation(libs.annotations)

    implementation("bisq:i18n:2.1.2")

    implementation("bisq:persistence:2.1.2")

    implementation("bisq:security:2.1.2")
    implementation(libs.bouncycastle)
    implementation(libs.bouncycastle.pg)
    testImplementation(libs.apache.commons.lang)
    implementation(libs.google.guava)

    // network
    implementation("network:network-identity:2.1.2")
    implementation("network:socks5-socket-channel:2.1.2")
    implementation("network:i2p:2.1.2")
    implementation("network:network:2.1.2")
    implementation(libs.chimp.jsocks)
    implementation(libs.failsafe)
    implementation(libs.apache.httpcomponents.httpclient)

    //tor
    implementation(libs.tukaani)
    implementation(libs.chimp.jsocks)
    implementation(libs.chimp.jtorctl)

    //i2p
    implementation(libs.bundles.i2p)

    implementation("bisq:identity:2.1.2") // cannot be used until network dependencies are fixed. -> Could not find network:network-common:.

    implementation("bisq:account:2.1.2") {
        exclude(group = "com.google.protobuf", module = "*")
    }

    implementation("bisq:settings:2.1.2")

    implementation("bisq:bonded-roles:2.1.2")
    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)

    implementation("bisq:user:2.1.2")
    implementation("bisq:contract:2.1.2")
    implementation("bisq:offer:2.1.2")
    implementation("bisq:trade:2.1.2")
    implementation("bisq:support:2.1.2")
    implementation("bisq:application:2.1.2")
    implementation("bisq:chat:2.1.2")
    implementation("bisq:presentation:2.1.2")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.slf4j.api)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    implementation(libs.protobuf.java)
    implementation(libs.protobuf.gradle.plugin)
    implementation("com.google.protobuf:protoc:3.25.4")
}

