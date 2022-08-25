buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
        classpath(libs.gradle.plugin.rust)
        classpath(libs.gradle.plugin.navigation)
    }
}

plugins {
    id("com.github.ben-manes.versions")
    id("com.osacky.fulladle")
    id("io.gitlab.arturbosch.detekt")
    id("zcash-sdk.ktlint-conventions")
    id("zcash-sdk.rosetta-conventions")
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

fladle {
    // Firebase Test Lab has min and max values that might differ from our project's
    // These are determined by `gcloud firebase test android models list`
    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MIN_API = 19

    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MAX_API = 33

    val minSdkVersion = run {
        val buildMinSdk = project.properties["ANDROID_MIN_SDK_VERSION"].toString().toInt()
        buildMinSdk.coerceAtLeast(FIREBASE_TEST_LAB_MIN_API).toString()
    }
    val targetSdkVersion = run {
        val buildTargetSdk = project.properties["ANDROID_TARGET_SDK_VERSION"].toString().toInt()
        buildTargetSdk.coerceAtMost(FIREBASE_TEST_LAB_MAX_API).toString()
    }

    val firebaseTestLabKeyPath = project.properties["ZCASH_FIREBASE_TEST_LAB_API_KEY_PATH"].toString()
    val firebaseProject = project.properties["ZCASH_FIREBASE_TEST_LAB_PROJECT"].toString()

    if (firebaseTestLabKeyPath.isNotEmpty()) {
        serviceAccountCredentials.set(File(firebaseTestLabKeyPath))
    } else if (firebaseProject.isNotEmpty()) {
        projectId.set(firebaseProject)
    }

    devices.addAll(
        mapOf("model" to "Nexus5", "version" to minSdkVersion),
        mapOf("model" to "Pixel2.arm", "version" to targetSdkVersion)
    )

    @Suppress("MagicNumber")
    flakyTestAttempts.set(2)

    flankVersion.set(libs.versions.flank.get())

    filesToDownload.set(listOf(
        "*/matrix_*/*test_results_merged\\.xml",
        "*/matrix_*/*/artifacts/sdcard/googletest/test_outputfiles/*\\.png"
    ))

    directoriesToPull.set(listOf(
        "/sdcard/googletest/test_outputfiles"
    ))
}
