plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":spring:spring-core"))
    implementation(project(":spring:adapters:webflux"))
    implementation(project(":spring:adapters:logging"))
    implementation(project(":spring:adapters:storage-file"))
    implementation(libs.spring.boot.starter.web)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
