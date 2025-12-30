plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
    id("publishing-convention")
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
