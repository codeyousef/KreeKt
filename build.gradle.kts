plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

group = "io.kreekt"
version = "0.1.0-alpha01"

kotlin {
    // JVM Target
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }

    // JS Target
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    // WASM Target (experimental)
    wasmJs {
        browser()
    }

    // Android Target
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }

    // iOS Targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // macOS Targets
    macosX64()
    macosArm64()

    // Windows Target
    mingwX64()

    // Linux Target
    linuxX64()

    // Configure source sets
    sourceSets {
        // Common source set
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.atomicfu)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // Desktop shared code (JVM)
        val jvmMain by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.vulkan)
            }
        }

        // Web/Browser
        val jsMain by getting {
            dependencies {
                implementation(npm("@webgpu/types", "0.1.40"))
            }
        }

        // Native shared code
        val nativeMain by creating {
            dependsOn(commonMain)
        }

        // Apple shared code
        val appleMain by creating {
            dependsOn(nativeMain)
        }

        val iosX64Main by getting {
            dependsOn(appleMain)
        }
        val iosArm64Main by getting {
            dependsOn(appleMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(appleMain)
        }
        val macosX64Main by getting {
            dependsOn(appleMain)
        }
        val macosArm64Main by getting {
            dependsOn(appleMain)
        }

        // Mobile shared code
        val mobileMain by creating {
            dependsOn(commonMain)
        }

        val androidMain by getting {
            dependsOn(mobileMain)
        }
    }
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    namespace = "io.kreekt"
}
