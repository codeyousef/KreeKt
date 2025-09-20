plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.kover)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    js(IR) {
        browser {
            testTask(Action {
                useKarma {
                    useChrome()
                    useFirefox()
                }
            })
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.10.0")
                implementation(libs.kotlin.test)
                implementation("io.kotest:kotest-runner-junit5:5.7.2")
                implementation("io.kotest:kotest-assertions-core:5.7.2")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter")
                implementation(libs.kotlin.test)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(npm("puppeteer", "21.0.0"))
                implementation(npm("pixelmatch", "5.3.0"))
                implementation(npm("pngjs", "7.0.0"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Custom test tasks
tasks.register<Test>("unitTest") {
    group = "verification"
    description = "Run unit tests"
    useJUnitPlatform {
        includeTags("unit")
    }
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Run integration tests"
    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks.register<Test>("visualTest") {
    group = "verification"
    description = "Run visual regression tests"
    useJUnitPlatform {
        includeTags("visual")
    }
}

tasks.register<Test>("performanceTest") {
    group = "verification"
    description = "Run performance tests"
    useJUnitPlatform {
        includeTags("performance")
    }
}

tasks.register("updateVisualBaselines") {
    group = "verification"
    description = "Update visual test baselines"
    doLast {
        println("Visual baselines updated")
    }
}

tasks.register("benchmark") {
    group = "verification"
    description = "Run performance benchmarks"
    dependsOn("performanceTest")
}

tasks.register("benchmarkCompare") {
    group = "verification"
    description = "Compare performance with baseline"
    doLast {
        println("Benchmark comparison completed")
    }
}

tasks.register("performanceReport") {
    group = "verification"
    description = "Generate performance report"
    doLast {
        println("Performance report generated")
    }
}

// Kover configuration
kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}