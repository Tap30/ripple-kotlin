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

rootProject.name = "ripple-kotlin"

include(
    ":core",
    ":android",
    ":spring",
    ":reactive",
    ":samples:android-sample",
    ":samples:spring-sample"
)
