plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        withJava()
    }

    js(IR) {
        browser {
            webpackTask {
                outputFileName = "performance-profiler.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
                implementation("org.slf4j:slf4j-api:2.0.9")
                implementation("ch.qos.logback:logback-classic:1.4.11")
            }
        }

        jsMain {
            dependencies {
                implementation(npm("@webgpu/types", "0.1.40"))
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

tasks.register("run") {
    group = "tools"
    description = "Run the performance profiler"
    dependsOn("jvmRun")
}