pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").orElse("2.0.21")
    val artifactoryUsername = System.getenv("ARTIFACTORY_ANDROID_USERNAME")
    val artifactoryPassword = System.getenv("ARTIFACTORY_ANDROID_PASSWORD")

    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.myket.ir")
        }
        if (artifactoryUsername != null && artifactoryPassword != null) {
            maven {
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
                url = uri("https://artifactory.tapsi.tech/artifactory/android-gradle-maven")
            }
        }
    }

    // The version catalog supplies the default toolchain. These properties let CI
    // verify the SDK with an additional Kotlin compiler without duplicating every
    // plugin declaration in the project.
    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.id == "org.jetbrains.kotlin.plugin.serialization" ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:${kotlinVersion.get()}")
                requested.id.id == "org.jetbrains.kotlin.plugin.spring" ->
                    useModule("org.jetbrains.kotlin:kotlin-allopen:${kotlinVersion.get()}")
                requested.id.id == "org.jetbrains.kotlin.plugin.compose" ->
                    useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${kotlinVersion.get()}")
                requested.id.id.startsWith("org.jetbrains.kotlin.") ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion.get()}")
            }
        }
    }
}

rootProject.name = "ripple-kotlin"

val kotlinVersion = providers.gradleProperty("kotlinVersion").orElse("2.0.21").get()

include(
    ":core",

    // JVM platform core modules
    ":spring:spring-core",
    ":reactive:reactive-core",

    // JVM platform adapter modules
    ":spring:adapters:webflux",
    ":spring:adapters:logging",
    ":spring:adapters:storage-file",
    ":reactive:adapters:reactor",

    // JVM sample modules
    ":samples:spring-sample",
    ":samples:spring-java-sample",
    ":samples:test-server",
)

// Android modules use Room/KSP and the Compose Compiler Gradle plugin, so Android
// is supported only by the Kotlin 2 toolchain. Kotlin 1 builds validate and
// publish the JVM-facing modules above.
if (!kotlinVersion.startsWith("1.")) {
    include(
        ":android:android-core",
        ":android:adapters:okhttp",
        ":android:adapters:logging",
        ":android:adapters:storage-room",
        ":android:adapters:storage-preferences",
        ":samples:android-sample",
    )
}
