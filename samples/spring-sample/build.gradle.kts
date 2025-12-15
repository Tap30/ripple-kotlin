plugins {
    id("kotlin-jvm-convention")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":spring"))
    implementation(libs.spring.boot.starter.web)
}
