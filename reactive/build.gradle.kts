plugins {
    id("kotlin-jvm-convention")
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.reactor.core)
}
