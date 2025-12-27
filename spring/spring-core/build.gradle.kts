plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("publishing-convention")
}

// Disable bootJar since this is a library, not an application
tasks.bootJar {
    enabled = false
}

// Enable plain jar
tasks.jar {
    enabled = true
    archiveClassifier = ""
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    
    // Spring Boot dependencies (compileOnly to avoid forcing on consumers)
    compileOnly(libs.spring.boot.starter.webflux)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.slf4j.api)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
