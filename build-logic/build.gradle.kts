plugins {
    `kotlin-dsl`
}

val artifactoryUsername = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
val artifactoryPassword = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    if (artifactoryUsername != null && artifactoryPassword != null) {
        maven {
            credentials {
                username = artifactoryUsername
                password = artifactoryPassword
            }
            url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
        }
    }
    maven {
        url = uri("https://maven.myket.ir")
    }
}
