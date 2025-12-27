plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":spring:spring-core"))
    implementation(project(":spring:adapters:webflux"))
    implementation(project(":spring:adapters:logging"))
    implementation(project(":spring:adapters:storage-file"))
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.jackson.module.kotlin)
}
