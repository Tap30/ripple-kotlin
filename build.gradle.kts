plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.nexus.publish)
}

// VERSION_NAME comes from gradle.properties locally or ORG_GRADLE_PROJECT_VERSION_NAME in CI.
allprojects {
    group = "cab.tapsi.oss"
    version = property("VERSION_NAME") as String

    repositories {
        google()
        mavenCentral()
        // Add GitHub Packages for consuming dependencies
        maven {
            url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
            credentials {
                username =
                    System.getenv("GITHUB_ACTOR") ?: findProperty("githubUsername") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken") as String?
            }
        }
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

            username.set(
                findProperty("centralPortalUsername") as String?
                    ?: findProperty("ossrhUsername") as String?
                    ?: System.getenv("CENTRAL_PORTAL_USERNAME")
                    ?: System.getenv("OSSRH_USERNAME")
            )
            password.set(
                findProperty("centralPortalPassword") as String?
                    ?: findProperty("ossrhPassword") as String?
                    ?: System.getenv("CENTRAL_PORTAL_PASSWORD")
                    ?: System.getenv("OSSRH_PASSWORD")
            )
        }
    }
}
