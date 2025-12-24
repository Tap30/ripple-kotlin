plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
    id("publishing-convention")
}

dependencies {
    api(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.2.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation(libs.kotlinx.coroutines.reactive)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
