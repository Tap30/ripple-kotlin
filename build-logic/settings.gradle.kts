pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
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

rootProject.name = "ripple-build-logic"
