pluginManagement {
    repositories {
        gradlePluginPortal()
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
        jcenter() // still needed by one dependency in the demo-app project
    }
}

rootProject.name = "zcash-android-sdk"

include("sdk-lib")
include("demo-app")