plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    id("maven-publish")
}

// Apply plugins to all subprojects
subprojects {
    apply(plugin = "org.jetbrains.dokka")

    if (name.startsWith("tools")) {
        group = "io.kreekt.tools"
        version = rootProject.version
    }
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
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
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

                // Advanced 3D features dependencies
                implementation(libs.kotlinx.datetime)

                // Math and collections
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6")

                // Asset loading and compression
                implementation("com.squareup.okio:okio:3.6.0")
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

                // Physics: Will be implemented with expect/actual pattern
                // implementation("org.lwjgl:lwjgl-bullet:3.3.3") // TODO: Add when available

                // Asset loading: Will use platform-specific implementations
                // implementation("org.lwjgl:lwjgl-draco:3.3.3") // TODO: Add when available

                // Font loading: Will use platform-specific implementations
                // implementation("org.lwjgl:lwjgl-freetype:3.3.3") // TODO: Add when available
            }
        }

        // Web/Browser
        val jsMain by getting {
            dependencies {
                implementation(npm("@webgpu/types", "0.1.40"))

                // Physics: Will be implemented with expect/actual pattern
                // implementation(npm("@dimforge/rapier3d-compat", "0.12.0")) // TODO: Add when needed

                // Asset loading: Will use platform-specific implementations
                // implementation(npm("draco3dgltf", "1.5.6")) // TODO: Add when needed

                // Font loading: Will use platform-specific implementations
                // implementation(npm("opentype.js", "1.3.4")) // TODO: Add when needed

                // XR: Will be implemented with expect/actual pattern
                // implementation(npm("webxr-polyfill", "2.0.3")) // TODO: Add when needed
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
            dependencies {
                // XR: Will be implemented with expect/actual pattern
                // implementation("com.google.ar:core:1.42.0") // TODO: Add when needed
            }
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
