pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.2"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        // id("kotlin-kapt") version "2.0.21"
        // Nếu bạn dùng KSP thì mở comment dòng dưới:
        id("com.google.devtools.ksp") version "2.0.21-1.0.25"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"


    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TodoApp"
include(":app")
