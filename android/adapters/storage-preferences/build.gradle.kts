plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("publishing-convention")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    
    // Use provided scope for Android dependencies
    compileOnly("com.google.android:android:4.1.1.4")
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
