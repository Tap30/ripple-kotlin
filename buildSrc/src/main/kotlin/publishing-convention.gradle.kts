import gradle.kotlin.dsl.accessors._d89d17b823009f0d56c867f95dae04f1.javadoc
import gradle.kotlin.dsl.accessors._d89d17b823009f0d56c867f95dae04f1.main
import gradle.kotlin.dsl.accessors._d89d17b823009f0d56c867f95dae04f1.sourceSets

plugins {
    `maven-publish`
    signing
    `java-library`
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            
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
    }
    
    repositories {
        // GitHub Packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
            credentials {
                // Use GitHub Actions default environment variables
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("githubUsername") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken") as String?
            }
        }
        
        // Tapsi Internal Artifactory
        maven {
            name = "TapsiArtifactory"
            url = uri("https://artifactory.tapsi.tech/artifactory/tap30-release")
            credentials {
                username = System.getenv("ARTIFACTORY_USERNAME") ?: findProperty("artifactoryUsername") as String?
                password = System.getenv("ARTIFACTORY_PASSWORD") ?: findProperty("artifactoryPassword") as String?
            }
        }
        
        // TODO: Uncomment when ready for Maven Central
        // maven {
        //     name = "sonatype"
        //     val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
        //     val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
        //     url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        //     
        //     credentials {
        //         username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
        //         password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
        //     }
        // }
    }
}

signing {
    // TODO: Set up GPG signing for Maven Central - see PUBLISHING.md for instructions
    // val signingKeyId = findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
    // val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
    // val signingSecretKeyRingFile = findProperty("signing.secretKeyRingFile") as String? ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    // 
    // if (signingKeyId != null && signingPassword != null && signingSecretKeyRingFile != null) {
    //     sign(publishing.publications["maven"])
    // }
}
