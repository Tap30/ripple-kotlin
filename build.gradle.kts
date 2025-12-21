plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.nexus.publish)
}

// Set version from gradle.properties
allprojects {
    group = "com.tapsioss.ripple"
    version = property("VERSION_NAME") as String

    repositories {
        google()
        mavenCentral()
    }
}

// Configure Maven Central publishing
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            
            // TODO: Set these credentials in ~/.gradle/gradle.properties or GitHub secrets
            // ossrhUsername=your_sonatype_username
            // ossrhPassword=your_sonatype_password
            username.set(findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME"))
            password.set(findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD"))
        }
    }
}
