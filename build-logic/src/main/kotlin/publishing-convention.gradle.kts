import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.Sign

plugins {
    `maven-publish`
    signing
    `java-library`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(rippleArtifactId())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = rippleArtifactId()

            configureRipplePom()
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("githubUsername") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken") as String?
            }
        }

        maven {
            name = "TapsiArtifactory"
            url = uri("https://artifactory.tapsi.tech/artifactory/tap30-release")
            credentials {
                username = System.getenv("ARTIFACTORY_USERNAME") ?: findProperty("artifactoryUsername") as String?
                password = System.getenv("ARTIFACTORY_PASSWORD") ?: findProperty("artifactoryPassword") as String?
            }
        }
    }
}

configureSigning()

fun Project.rippleArtifactId(): String {
    val baseName = if (parent?.name == "adapters" && parent?.parent != null) {
        "${parent!!.parent!!.name}-adapters-$name"
    } else {
        name
    }
    return "ripple-$baseName"
}

fun MavenPublication.configureRipplePom() {
    pom {
        name.set("Ripple Kotlin SDK - ${project.name}")
        description.set("High-performance, scalable, and fault-tolerant event tracking SDK for Kotlin and Java applications")
        url.set("https://github.com/Tap30/ripple-kotlin")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("tap30")
                name.set("Tap30 Team")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/Tap30/ripple-kotlin.git")
            developerConnection.set("scm:git:ssh://github.com:Tap30/ripple-kotlin.git")
            url.set("https://github.com/Tap30/ripple-kotlin/tree/main")
        }
    }
}

fun Project.configureSigning() {
    val signingKey = findProperty("signingInMemoryKey") as String?
        ?: System.getenv("SIGNING_KEY")
        ?: System.getenv("SIGNING_SECRET_KEY")
    val signingPassword = findProperty("signingInMemoryKeyPassword") as String?
        ?: findProperty("signing.password") as String?
        ?: System.getenv("SIGNING_PASSWORD")

    signing {
        isRequired = signingKey != null && gradle.taskGraph.allTasks.any { task ->
            task.name.contains("publish", ignoreCase = true) ||
                task.name.contains("closeAndRelease", ignoreCase = true)
        }

        if (signingKey != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["maven"])
        }
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { signingKey != null }
    }
}
