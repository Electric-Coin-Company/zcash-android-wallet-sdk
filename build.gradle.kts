buildscript {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        //noinspection GradlePluginVersion
        classpath("com.android.tools.build:gradle:${properties["ANDROID_GRADLE_PLUGIN_VERSION"]}")
        classpath("org.mozilla.rust-android-gradle:plugin:0.9.0")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.17.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:_")
    }
}

plugins {
    id("org.jetbrains.dokka")
    id("org.owasp.dependencycheck")
    id("zcash.ktlint-conventions")
    id("io.gitlab.arturbosch.detekt")
}

tasks {
    register("detektAll", io.gitlab.arturbosch.detekt.Detekt::class) {
        parallel = true
        setSource(files(projectDir))
        include("**/*.kt")
        include("**/*.kts")
        exclude("**/resources/**")
        exclude("**/build/**")
        exclude("**/commonTest/**")
        exclude("**/jvmTest/**")
        exclude("**/androidTest/**")
        config.setFrom(files("${rootProject.projectDir}/tools/detekt.yml"))
        buildUponDefaultConfig = true
    }
}