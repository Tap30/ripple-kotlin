plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("android") version "1.9.21" apply false
    kotlin("plugin.spring") version "1.9.21" apply false
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
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
