plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
    id("publishing-convention")
}

// Configure JAR name to match artifactId
tasks.jar {
    archiveBaseName.set("spring-adapters-webflux")
}

// Configure artifact name to avoid conflicts
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "spring-adapters-webflux"
        }
    }
}

dependencies {
    api(project(":spring:spring-core"))
    implementation(libs.boot.spring.boot.starter.webflux)
    implementation(libs.module.jackson.module.kotlin)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
