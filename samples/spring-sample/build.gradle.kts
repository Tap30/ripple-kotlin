plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.9.21"
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation(project(":spring"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
