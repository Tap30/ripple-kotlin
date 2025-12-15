plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.tapsioss.ripple.sample.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tapsioss.ripple.sample.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    implementation(project(":android"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
