plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

// Configure JAR name to match artifactId
tasks.jar {
    archiveBaseName.set("spring-adapters-storage-file")
}

// Configure artifact name to avoid conflicts
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "spring-adapters-storage-file"
        }
    }
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.module.jackson.module.kotlin)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
