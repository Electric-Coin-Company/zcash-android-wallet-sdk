import org.jetbrains.kotlin.config.KotlinCompilerVersion

apply(from = "custom-tasks.gradle.kts")

plugins {
    id("com.android.library")
    id("kotlin-android-extensions")
    id("kotlin-android")
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets["main"].java.srcDirs("build/generated/source/wire")
}

dependencies {
    api("com.squareup.wire:wire-runtime:2.2.0")

    implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("com.android.support:appcompat-v7:28.0.0")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}