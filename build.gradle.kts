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
    id("com.github.ben-manes.versions")
    id("com.osacky.fulladle")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.dokka")
    id("org.owasp.dependencycheck")
    id("zcash-sdk.ktlint-conventions")
}

apply(plugin = "com.vanniktech.maven.publish")

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
        baseline.set(file("$rootDir/tools/detekt-baseline.xml"))
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

// Firebase Test Lab has min and max values that might differ from our project's
// These are determined by `gcloud firebase test android models list`
@Suppress("MagicNumber", "PropertyName", "VariableNaming")
val FIREBASE_TEST_LAB_MIN_API = 23
@Suppress("MagicNumber", "PropertyName", "VariableNaming")
val FIREBASE_TEST_LAB_MAX_API = 30

val firebaseTestLabKeyPath = project.properties["ZCASH_FIREBASE_TEST_LAB_API_KEY_PATH"].toString()
if (firebaseTestLabKeyPath.isNotBlank()) {
    val minSdkVersion = run {
        val buildMinSdk = project.properties["ANDROID_MIN_SDK_VERSION"].toString().toInt()
        buildMinSdk.coerceAtLeast(FIREBASE_TEST_LAB_MIN_API).toString()
    }
    val targetSdkVersion = run {
        val buildTargetSdk = project.properties["ANDROID_TARGET_SDK_VERSION"].toString().toInt()
        buildTargetSdk.coerceAtMost(FIREBASE_TEST_LAB_MAX_API).toString()
    }
    fladle {
        serviceAccountCredentials.set(File(firebaseTestLabKeyPath))
        devices.addAll(
            mapOf("model" to "NexusLowRes", "version" to minSdkVersion),
            mapOf("model" to "NexusLowRes", "version" to targetSdkVersion)
        )

        @Suppress("MagicNumber")
        flakyTestAttempts.set(2)

        flankVersion.set(libs.versions.flank.get())
    }
}