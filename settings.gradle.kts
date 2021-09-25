enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }

    plugins {
        val detektVersion = extra["DETEKT_VERSION"].toString()
        val dokkaVersion = extra["DOKKA_VERSION"].toString()
        val kotlinVersion = extra["KOTLIN_VERSION"].toString()
        val owaspVersion = extra["OWASP_DEPENDENCY_CHECK_VERSION"].toString()
        val protobufVersion = extra["PROTOBUF_GRADLE_PLUGIN_VERSION"].toString()

        id("com.google.protobuf") version(protobufVersion) apply(false)
        id("org.jetbrains.dokka") version(dokkaVersion) apply(false)
        id("org.jetbrains.kotlin.plugin.allopen") version(kotlinVersion) apply(false)
        id("org.owasp.dependencycheck") version(owaspVersion) apply(false)
        id("io.gitlab.arturbosch.detekt") version (detektVersion) apply (false)
    }
}

plugins {
    id("de.fayard.refreshVersions") version("0.20.0")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }

    @Suppress("UnstableApiUsage", "MaxLineLength")
    versionCatalogs {
        create("libs") {
            val grpcVersion = extra["GRPC_VERSION"].toString()
            val javaVersion = extra["ANDROID_JVM_TARGET"].toString()
            val protocVersion = extra["PROTOC_VERSION"].toString()

            // Standalone versions
            version("grpc", grpcVersion)
            version("java", javaVersion)
            version("protoc", protocVersion)
        }
    }
}

rootProject.name = "zcash-android-sdk"

includeBuild("build-conventions")

include("sdk-lib")
include("demo-app")