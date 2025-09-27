plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    // alias(libs.plugins.androidLibrary) // Disabled on Windows
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
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // JS Target
    js(IR) {
        browser {
            testTask(Action {
                useKarma {
                    useChromeHeadless()
                }
            })
        }
    }

    // Android Target - Disabled on Windows (requires Android SDK)
    // androidTarget {
    //     compilerOptions {
    //         jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    //     }
    // }

    // iOS Targets - Disabled on Windows
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    // macOS Targets - Disabled on Windows
    // macosX64()
    // macosArm64()

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
                implementation(libs.kotlinx.collections.immutable)

                // Asset loading and compression
                implementation(libs.okio)
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

        // Apple shared code - Disabled on Windows
        // val appleMain by creating {
        //     dependsOn(nativeMain)
        // }

        // val iosX64Main by getting {
        //     dependsOn(appleMain)
        // }
        // val iosArm64Main by getting {
        //     dependsOn(appleMain)
        // }
        // val iosSimulatorArm64Main by getting {
        //     dependsOn(appleMain)
        // }
        // val macosX64Main by getting {
        //     dependsOn(appleMain)
        // }
        // val macosArm64Main by getting {
        //     dependsOn(appleMain)
        // }

        // Linux Target
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }

        // Windows Target
        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }

        // Mobile shared code
        // val mobileMain by creating {
        //     dependsOn(commonMain)
        // }

        // val androidMain by getting {
        //     dependsOn(mobileMain)
        //     dependencies {
        //         // XR: Will be implemented with expect/actual pattern
        //         // implementation("com.google.ar:core:1.42.0") // TODO: Add when needed
        //     }
        // }
    }
}

// android {
//     compileSdk = libs.versions.android.compileSdk.get().toInt()
//     sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
//     defaultConfig {
//         minSdk = libs.versions.android.minSdk.get().toInt()
//     }
//
//     testOptions {
//         targetSdk = libs.versions.android.targetSdk.get().toInt()
//     }
//     namespace = "io.kreekt"
//
//     compileOptions {
//         sourceCompatibility = JavaVersion.VERSION_11
//         targetCompatibility = JavaVersion.VERSION_11
//     }
// }
