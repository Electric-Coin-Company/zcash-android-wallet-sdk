buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
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
    id("com.github.ben-manes.versions")
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

    withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
        gradleReleaseChannel = "current"

        resolutionStrategy {
            componentSelection {
                all {
                    if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                        reject("Unstable")
                    }
                }
            }
        }
    }
}

val unstableKeywords = listOf("alpha", "beta", "rc", "m", "ea", "build")

fun isNonStable(version: String): Boolean {
    val versionLowerCase = version.toLowerCase()

    return unstableKeywords.any { versionLowerCase.contains(it) }
}
