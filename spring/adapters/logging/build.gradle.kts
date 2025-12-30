plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

dependencies {
    api(project(":core"))
    implementation(libs.slf4j.slf4j.api)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
