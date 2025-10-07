plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()

    js(IR) {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }

            // Configure webpack for Web Worker support
            webpackTask {
                mainOutputFileName.set("voxelcraft.js")
            }
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
                implementation(project(":"))
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

        val jvmMain by getting {
            dependencies {
                val lwjglVersion = "3.3.4"
                val lwjglNatives = "natives-linux"

                implementation("org.lwjgl:lwjgl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")

                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

// ============================================================================
// Run Tasks for VoxelCraft
// ============================================================================

tasks.register("runJs") {
    group = "examples"
    description = "Run VoxelCraft in browser"

    dependsOn("jsBrowserDevelopmentRun")

    doFirst {
        println("🎮 Starting VoxelCraft (Minecraft Clone)")
        println("Opening in your default browser...")
        println("Controls: WASD=Move, Mouse=Look, F=Flight, Left Click=Break, Right Click=Place")
    }
}

tasks.register("dev") {
    group = "examples"
    description = "Development mode - continuous build and run"

    dependsOn("jsBrowserDevelopmentRun")

    doFirst {
        println("🔄 Starting VoxelCraft development mode with hot reload")
    }
}

tasks.register("buildJs") {
    group = "examples"
    description = "Build production JavaScript bundle"

    dependsOn("jsBrowserProductionWebpack")

    doFirst {
        println("📦 Building VoxelCraft production bundle...")
    }
}

tasks.register<JavaExec>("runJvm") {
    group = "examples"
    description = "Run VoxelCraft on JVM with LWJGL/OpenGL"

    dependsOn("jvmJar")

    classpath = files(
        tasks.named("jvmJar").get().outputs.files,
        configurations.getByName("jvmRuntimeClasspath")
    )

    mainClass.set("io.kreekt.examples.voxelcraft.MainJvmKt")

    // Add JVM args for LWJGL native access on Java 17+
    jvmArgs = listOf(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
    )

    doFirst {
        println("🎮 Starting VoxelCraft (JVM/LWJGL)")
        println("Controls: WASD=Move, Mouse=Look, F=Flight, Space/Shift=Up/Down, ESC=Quit")
    }
}
