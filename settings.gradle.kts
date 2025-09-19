pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kreekt"

// Tool modules
include(":tools:editor")
include(":tools:profiler")
include(":tools:tests")
include(":tools:docs")
include(":tools:cicd")
