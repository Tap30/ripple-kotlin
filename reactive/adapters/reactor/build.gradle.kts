plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

// Configure JAR name to match artifactId
tasks.jar {
    archiveBaseName.set("reactive-adapters-reactor")
}

// Configure artifact name to avoid conflicts
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "reactive-adapters-reactor"
        }
    }
}

dependencies {
    api(project(":reactive:reactive-core"))
    implementation(libs.reactor.core)
    implementation(libs.kotlinx.coroutines.reactive)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
