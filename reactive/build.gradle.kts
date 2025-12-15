plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
    implementation("io.projectreactor:reactor-core:3.6.0")
}
