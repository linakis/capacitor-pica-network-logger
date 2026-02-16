plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
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
        val iosMain by creating {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
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
