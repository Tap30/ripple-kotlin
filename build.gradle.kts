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

// Get version from git tags
fun getVersionFromGit(): String {
    return try {
        val stdout = java.io.ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--tags", "--always", "--dirty")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val version = stdout.toString().trim()
        
        when {
            version.isEmpty() -> "0.1.0-SNAPSHOT"
            version.startsWith("v") -> version.substring(1)
            else -> "$version-SNAPSHOT"
        }
    } catch (e: Exception) {
        "0.1.0-SNAPSHOT"
    }
}

// Set version from git tags
allprojects {
    group = "com.tapsioss.ripple"
    version = getVersionFromGit()

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
