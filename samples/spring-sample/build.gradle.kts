plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":spring:spring-core"))
    implementation(project(":spring:adapters:webflux"))
    implementation(project(":spring:adapters:logging"))
    implementation(project(":spring:adapters:storage-file"))
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.coroutines.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
