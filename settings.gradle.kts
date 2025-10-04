pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kreekt"

// Tool modules - Temporarily disabled on Windows due to compilation issues
// include(":tools:editor")
// include(":tools:profiler")
// include(":tools:tests")
// include(":tools:docs")
// include(":tools:cicd")
// include(":tools:api-server")

// Library modules (future expansion)
// kreekt-postprocessing needs architectural fixes - temporarily disabled
// include(":kreekt-postprocessing")

// Validation module
include(":kreekt-validation")

// Example projects
include(":examples:basic-scene")
