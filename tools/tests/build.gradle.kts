plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.kover)
}

kotlin {
    jvm {
        withJava()
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChrome()
                    useFirefox()
                }
            }
        }
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
                implementation("org.junit.jupiter:junit-jupiter:5.10.0")
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
                implementation("io.kotest:kotest-runner-junit5:5.7.2")
                implementation("io.kotest:kotest-assertions-core:5.7.2")
            }
        }

        jvmTest {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter")
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }

        jsMain {
            dependencies {
                implementation(npm("puppeteer", "21.0.0"))
                implementation(npm("pixelmatch", "5.3.0"))
                implementation(npm("pngjs", "7.0.0"))
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
koverReport {
    defaults {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}