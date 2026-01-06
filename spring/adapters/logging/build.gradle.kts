plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

// Configure JAR name to match artifactId
tasks.withType<Jar> {
    archiveBaseName.set("spring-adapters-logging")
}

// Configure artifact name to avoid conflicts
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "spring-adapters-logging"
        }
    }
}

dependencies {
    api(project(":core"))
    implementation(libs.slf4j.slf4j.api)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
