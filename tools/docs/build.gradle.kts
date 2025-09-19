plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.dokka)
    application
}

dependencies {
    implementation(project(":"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Web server for documentation
    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-html-builder:2.3.4")

    // Documentation generation
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
    implementation("org.jetbrains.dokka:dokka-core:1.9.10")

    // Markdown processing
    implementation("org.jetbrains:markdown:0.5.0")

    // Search indexing
    implementation("org.apache.lucene:lucene-core:9.7.0")
    implementation("org.apache.lucene:lucene-analyzers-common:9.7.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set("io.kreekt.tools.docs.server.DocServerKt")
}

tasks.register("serve") {
    group = "tools"
    description = "Start documentation server"
    dependsOn("run")
}

// Dokka configuration will be added once the implementation is complete

tasks.register("docs") {
    group = "documentation"
    description = "Generate all documentation"
    dependsOn("dokkaHtml")
}

// Serve task already defined above