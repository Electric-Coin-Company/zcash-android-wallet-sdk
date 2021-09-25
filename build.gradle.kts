buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.gradle.plugin.android)
        classpath(libs.gradle.plugin.rust)
        classpath(libs.gradle.plugin.publish)
        classpath(libs.gradle.plugin.navigation)
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