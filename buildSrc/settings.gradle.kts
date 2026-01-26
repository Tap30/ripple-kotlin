pluginManagement {
    repositories {
        maven {
            credentials {
                username = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
                password = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")
            }
            url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
        }
        maven {
            url = uri("https://maven.myket.ir")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
