plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")

    id("org.jetbrains.dokka")
    id("org.mozilla.rust-android-gradle.rust-android")

    id("wtf.emulator.gradle")
    id("zcash-sdk.emulator-wtf-conventions")

    id("maven-publish")
    id("signing")
    id("zcash-sdk.publishing-conventions")
}

// Publishing information

val myVersion = project.property("LIBRARY_VERSION").toString()
val myArtifactId = "zcash-android-backend"
publishing {
    publications {
        publications.withType<MavenPublication>().all {
            artifactId = myArtifactId
        }
    }
}

android {
    namespace = "cash.z.ecc.android.backend"

    useLibrary("android.test.runner")

    defaultConfig {
        consumerProguardFiles("proguard-consumer.txt")
    }

    buildTypes {
        getByName("debug").apply {
            // test builds exceed the dex limit because they pull in large test libraries
            isMinifyEnabled = false
        }
        getByName("release").apply {
            isMinifyEnabled = project.property("IS_MINIFY_SDK_ENABLED").toString().toBoolean()
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-project.txt")
                )
            )
        }
        create("benchmark") {
            // We provide the extra benchmark build type just for benchmarking purposes
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    lint {
        baseline = File("lint-baseline.xml")
    }
}

cargo {
    module = "."
    libname = "zcashwalletsdk"
    targets = listOf(
        "arm",
        "arm64",
        "x86",
        "x86_64"
    )
    val minSdkVersion = project.property("ANDROID_MIN_SDK_VERSION").toString().toInt()
    apiLevels = mapOf(
        "arm" to minSdkVersion,
        "arm64" to minSdkVersion,
        "x86" to minSdkVersion,
        "x86_64" to minSdkVersion,
    )

    profile = "release"
    prebuiltToolchains = true

    // As a workaround to the Gradle (starting from v7.4.1) and Rust Android Gradle plugin (starting from v0.9.3)
    // incompatibility issue we need to add rust jni directory manually. See
    // https://github.com/mozilla/rust-android-gradle/issues/118
    tasks.whenObjectAdded {
        // This covers mergeDebugJniLibFolders, mergeReleaseJniLibFolders, etc.
        if (name.contains("^merge.+JniLibFolders$".toRegex())) {
            dependsOn("cargoBuild")
            // Fix for mergeDebugJniLibFolders UP-TO-DATE
            inputs.dir(buildDir.resolve("rustJniLibs/android"))
        }
    }
}

dependencies {
    api(projects.lightwalletClientLib)

    implementation(libs.androidx.annotation)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.kotlin.test)

    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.zcashwalletplgn)
    androidTestImplementation(libs.bip39)
}

tasks {
    /*
     * The Mozilla Rust Gradle plugin caches the native build data under the "target" directory,
     * which does not normally get deleted during a clean. The following task and dependency solves
     * that.
     */
    getByName<Delete>("clean").dependsOn(create<Delete>("cleanRustBuildOutput") {
        delete("target")
    })
}

project.afterEvaluate {
    val cargoTask = tasks.getByName("cargoBuild")
    tasks.getByName("javaPreCompileDebug").dependsOn(cargoTask)
    tasks.getByName("javaPreCompileRelease").dependsOn(cargoTask)
}

fun MinimalExternalModuleDependency.asCoordinateString() =
    "${module.group}:${module.name}:${versionConstraint.displayName}"