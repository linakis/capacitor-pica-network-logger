plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.native.cocoapods")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    cocoapods {
        name = "PicaNetworkLoggerShared"
        version = "0.1.0"
        summary = "Capacitor HTTP inspector shared UI"
        homepage = "https://github.com/linakis/capacitor-pica-network-logger"
        ios.deploymentTarget = "14.0"
        framework {
            baseName = "PicaNetworkLoggerShared"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("app.cash.sqldelight:runtime:2.0.2")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-ktx:1.9.2")
                implementation("androidx.activity:activity-compose:1.9.2")
                implementation("app.cash.sqldelight:android-driver:2.0.2")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
    }
}

android {
    namespace = "com.linakis.capacitorpicanetworklogger.kmp"
    compileSdk = 34
    defaultConfig {
        minSdk = 23
    }
}

sqldelight {
    databases {
        create("InspectorDatabase") {
            packageName.set("com.linakis.capacitorpicanetworklogger.kmp.db")
        }
    }
}
