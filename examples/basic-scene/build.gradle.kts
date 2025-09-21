plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                // LWJGL for OpenGL/Vulkan
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.vulkan)

                // Platform-specific natives
                val lwjglVersion = libs.versions.lwjgl.get()
                val osName = System.getProperty("os.name").lowercase()
                val nativePlatform = when {
                    osName.contains("windows") -> "natives-windows"
                    osName.contains("linux") -> "natives-linux"
                    osName.contains("mac") -> "natives-macos"
                    else -> "natives-linux" // Default fallback
                }

                implementation("org.lwjgl:lwjgl:$lwjglVersion:$nativePlatform")
                implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion:$nativePlatform")
                implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion:$nativePlatform")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}