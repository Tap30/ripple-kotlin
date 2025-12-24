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
    
    // Adapter modules
    ":ripple-android-okhttp",
    ":ripple-android-room",
    ":ripple-spring-webflux",
    ":ripple-reactive-reactor",
    
    // Sample modules
    ":samples:android-sample",
    ":samples:spring-sample",
    ":samples:spring-java-sample",
)
