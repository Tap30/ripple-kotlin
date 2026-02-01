plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

// Configure JAR name to match artifactId
tasks.withType<Jar> {
    archiveBaseName.set("android-adapters-logging")
}

// Configure artifact name to avoid conflicts
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "android-adapters-logging"
        }
    }
}

dependencies {
    api(project(":core"))
    
    // Use provided scope for Android dependencies
    compileOnly("com.google.android:android:4.1.1.4")
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
