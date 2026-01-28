plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.ksp) apply false
    // TODO: Uncomment when ready for Maven Central
    // alias(libs.plugins.nexus.publish)
}

// Set version from gradle.properties
allprojects {
    group = "io.github.tap30.ripple"
    version = property("VERSION_NAME") as String

    repositories {
        maven {
            url = uri("https://maven.myket.ir")
        }
        google()
        mavenCentral()
        maven {
            credentials {
                username = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
                password = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")
            }
            url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
        }
        // Add GitHub Packages for consuming dependencies
        maven {
            url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
            credentials {
                username =
                    System.getenv("GITHUB_ACTOR") ?: findProperty("githubUsername") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken") as String?
            }
        }
    }
}

// TODO: Uncomment when ready for Maven Central
// nexusPublishing {
//     repositories {
//         sonatype {
//             nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
//             snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
//             
//             username.set(findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME"))
//             password.set(findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD"))
//         }
//     }
// }
