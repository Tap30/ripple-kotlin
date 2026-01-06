plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

// Configure JAR name to match artifactId
tasks.jar {
    archiveBaseName.set("android-adapters-okhttp")
}

// Configure artifact name
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "android-adapters-okhttp"
        }
    }
}

dependencies {
    implementation(project(":core"))
    api(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

}

tasks.withType<Test> {
    useJUnitPlatform()
}
