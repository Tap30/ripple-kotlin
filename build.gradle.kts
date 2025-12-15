plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("android") version "1.9.21" apply false
    kotlin("plugin.spring") version "1.9.21" apply false
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.tapsioss.ripple"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    // Only apply Kotlin JVM plugin to non-Android modules
    if (name != "android" && !name.contains("android-sample")) {
        apply(plugin = "kotlin")
        
        dependencies {
            val implementation by configurations
            val testImplementation by configurations

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
            testImplementation("io.mockk:mockk:1.13.8")
            testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
}
