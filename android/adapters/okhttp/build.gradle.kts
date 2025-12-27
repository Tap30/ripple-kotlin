plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

}

tasks.withType<Test> {
    useJUnitPlatform()
}
