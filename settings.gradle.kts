pluginManagement {
    repositories {
        maven {
            credentials {
                username = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
                password = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")
            }
            url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "ripple-kotlin"

include(
    ":core",
    
    // Platform core modules
    ":android:android-core",
    ":spring:spring-core",
    ":reactive:reactive-core",
    
    // Platform adapter modules
    ":android:adapters:okhttp",
    ":android:adapters:room",
    ":android:adapters:logging",
    ":android:adapters:storage-preferences",
    ":spring:adapters:webflux",
    ":spring:adapters:logging",
    ":spring:adapters:storage-file",
    ":reactive:adapters:reactor",
    
    // Sample modules
    ":samples:android-sample",
    ":samples:spring-sample",
    ":samples:spring-java-sample",
    ":samples:test-server",
)
