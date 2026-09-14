plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.kotlin.plugin.spring") apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.nexus.publish)
}

val kotlinVersion = providers.gradleProperty("kotlinVersion").orElse("2.0.21")
val kotlinRuntimeArtifacts = setOf(
    "kotlin-reflect",
    "kotlin-script-runtime",
    "kotlin-stdlib",
    "kotlin-stdlib-common",
    "kotlin-stdlib-jdk7",
    "kotlin-stdlib-jdk8",
    "kotlin-test",
    "kotlin-test-common",
    "kotlin-test-junit",
    "kotlin-test-junit5",
)
val artifactoryUsername = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
val artifactoryPassword = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")

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
        if (artifactoryUsername != null && artifactoryPassword != null) {
            maven {
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
                url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
            }
        }
    }

    // Kotlin libraries compiled with a newer metadata version cannot be consumed
    // by the Kotlin 1.9 compiler. Keep the runtime aligned with the toolchain
    // selected through -PkotlinVersion, including transitive dependencies.
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name in kotlinRuntimeArtifacts) {
                useVersion(kotlinVersion.get())
            }
        }
    }

    // JUnit Jupiter 5.12 requires the matching JUnit Platform launcher at test
    // runtime. Gradle otherwise supplies an older launcher from its own runtime.
    pluginManager.withPlugin("java") {
        dependencies.add("testRuntimeOnly", libs.junit.platform.launcher)
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
