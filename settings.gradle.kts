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
    
    // Platform core modules
    ":android:core",
    ":spring:core", 
    ":reactive:core",
    
    // Platform adapter modules
    ":android:adapters:okhttp",
    ":android:adapters:room",
    ":spring:adapters:webflux",
    ":reactive:adapters:reactor",
    
    // Sample modules
    ":samples:android-sample",
    ":samples:spring-sample",
    ":samples:spring-java-sample",
)
