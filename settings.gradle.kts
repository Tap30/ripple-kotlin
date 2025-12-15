pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
