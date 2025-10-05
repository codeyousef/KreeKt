plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
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
