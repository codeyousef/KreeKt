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

// Tool modules
include(":tools:editor")
include(":tools:profiler")
include(":tools:tests")
include(":tools:docs")
include(":tools:cicd")
include(":tools:api-server")

// Example projects
include(":examples:basic-scene")
