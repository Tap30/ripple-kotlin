plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
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
