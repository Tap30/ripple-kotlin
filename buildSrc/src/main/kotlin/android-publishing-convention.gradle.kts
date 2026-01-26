plugins {
    `maven-publish`
    signing
}

// Disable automatic Javadoc generation for Android libraries
tasks.withType<Javadoc> {
    enabled = false
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                
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
        }
    }
}
