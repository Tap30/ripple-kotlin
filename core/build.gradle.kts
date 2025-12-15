plugins {
    id("kotlin-jvm-convention")
    `java-library`
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
