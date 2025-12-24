plugins {
    `maven-publish`
    signing
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
                            email.set("developers@tap30.org")
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
    }
}

signing {
    val signingKeyId = findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
    val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
    val signingSecretKeyRingFile = findProperty("signing.secretKeyRingFile") as String? ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    
    if (signingKeyId != null && signingPassword != null && signingSecretKeyRingFile != null) {
        sign(publishing.publications["maven"])
    }
}
