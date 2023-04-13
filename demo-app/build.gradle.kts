plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs")
    id("com.osacky.fladle")
}

android {
    namespace = "cash.z.ecc.android.sdk.demoapp"

    defaultConfig {
        applicationId = "cash.z.ecc.android.sdk.demoapp"
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.androidx.compose.compiler.get().versionConstraint.displayName
    }

    val releaseKeystorePath = project.property("ZCASH_RELEASE_KEYSTORE_PATH").toString()
    val releaseKeystorePassword = project.property("ZCASH_RELEASE_KEYSTORE_PASSWORD").toString()
    val releaseKeyAlias = project.property("ZCASH_RELEASE_KEY_ALIAS").toString()
    val releaseKeyAliasPassword =
        project.property("ZCASH_RELEASE_KEY_ALIAS_PASSWORD").toString()
    val isReleaseSigningConfigured = listOf(
        releaseKeystorePath,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyAliasPassword
    ).all { it.isNotBlank() }

    signingConfigs {
        if (isReleaseSigningConfigured) {
            // If this block doesn't execute, the output will be unsigned
            create("release").apply {
                storeFile = File(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyAliasPassword
            }
        }
    }

    flavorDimensions.add("network")

    productFlavors {
        // would rather name them "testnet" and "mainnet" but product flavor names cannot start with the word "test"
        create("zcashtestnet") {
            dimension = "network"
            applicationId = "cash.z.ecc.android.sdk.demoapp.testnet"
            matchingFallbacks.addAll(listOf("zcashtestnet", "debug"))
        }

        create("zcashmainnet") {
            dimension = "network"
            applicationId = "cash.z.ecc.android.sdk.demoapp.mainnet"
            matchingFallbacks.addAll(listOf("zcashmainnet", "release"))
        }
    }

    buildTypes {
        getByName("release").apply {
            isMinifyEnabled = project.property("IS_MINIFY_APP_ENABLED").toString().toBoolean()
            isShrinkResources = project.property("IS_MINIFY_APP_ENABLED").toString().toBoolean()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-project.txt"
            )
            val isSignReleaseBuildWithDebugKey = project.property("IS_SIGN_RELEASE_BUILD_WITH_DEBUG_KEY")
                .toString().toBoolean()

            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            } else if (isSignReleaseBuildWithDebugKey) {
                // Warning: in this case the release build signed with the debug key
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        create("benchmark") {
            // We provide the extra benchmark build type just for benchmarking purposes
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")

            // To enable debugging while running benchmark tests, although it reduces their performance
            if (project.property("IS_DEBUGGABLE_WHILE_BENCHMARKING").toString().toBoolean()) {
                isDebuggable = true
            }
        }
    }

    lint {
        baseline = File("lint-baseline.xml")
    }
}

dependencies {
    // SDK
    implementation(projects.sdkLib)
    implementation(projects.sdkIncubatorLib)

    // sample mnemonic plugin
    implementation(libs.zcashwalletplgn)
    implementation(libs.bip39)

    // Android
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.security.crypto)
    implementation(libs.bundles.androidx.compose.core)
    implementation(libs.bundles.androidx.compose.extended)
    implementation(libs.material)

    // Just to support profile installation and tracing events needed by benchmark tests
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing)

    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.androidx.compose.test.junit)
    androidTestImplementation(libs.androidx.compose.test.manifest)
    androidTestImplementation(libs.kotlin.reflect)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlin.test)

    implementation(libs.bundles.grpc)
    implementation(libs.kotlinx.datetime)
}

fladle {
    // Firebase Test Lab has min and max values that might differ from our project's
    // These are determined by `gcloud firebase test android models list`
    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MIN_API = 27 // Minimum for Pixel2.arm device

    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MAX_API = 33

    val minSdkVersion = run {
        val buildMinSdk =
            project.properties["ANDROID_MIN_SDK_VERSION"].toString().toInt()
        buildMinSdk.coerceAtLeast(FIREBASE_TEST_LAB_MIN_API).toString()
    }
    val targetSdkVersion = run {
        val buildTargetSdk =
            project.properties["ANDROID_TARGET_SDK_VERSION"].toString().toInt()
        buildTargetSdk.coerceAtMost(FIREBASE_TEST_LAB_MAX_API).toString()
    }

    val firebaseTestLabKeyPath = project.properties["ZCASH_FIREBASE_TEST_LAB_API_KEY_PATH"].toString()
    val firebaseProject = project.properties["ZCASH_FIREBASE_TEST_LAB_PROJECT"].toString()

    if (firebaseTestLabKeyPath.isNotEmpty()) {
        serviceAccountCredentials.set(File(firebaseTestLabKeyPath))
    } else if (firebaseProject.isNotEmpty()) {
        projectId.set(firebaseProject)
    }

    configs {
        create("sanityConfig") {
            clearPropertiesForSanityRobo()

            debugApk.set(
                project.provider {
                    "${buildDir}/outputs/apk/zcashmainnet/release/demo-app-zcashmainnet-release.apk"
                }
            )

            testTimeout.set("5m")

            devices.addAll(
                mapOf("model" to "Pixel2.arm", "version" to minSdkVersion),
                mapOf("model" to "Pixel2.arm", "version" to targetSdkVersion)
            )

            flankVersion.set(libs.versions.flank.get())
        }
    }
}

// This is a workaround for issue #723.
// Native libraries are missing after this: `./gradlew clean; ./gradlew :demo-app:assemble`
// But are present after this: `./gradlew clean; ./gradlew assemble`
// The second one probably doesn't solve the problem, as there's probably a race condition in the Rust Gradle Plugin.
// This hack ensures that the SDK is completely built before the demo app starts being built.  There may be more
// efficient or better solutions we can find later.
tasks.getByName("assemble").dependsOn(":sdk-lib:assemble")
