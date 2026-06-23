pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.myket.ir")
        }
        maven {
            credentials {
                username = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
                password = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")
            }
            url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
        }
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
    ":android:adapters:logging",
    ":android:adapters:storage-room",
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
