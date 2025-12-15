plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.tapsioss.ripple.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(project(":core"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
}
