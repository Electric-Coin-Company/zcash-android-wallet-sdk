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
        minSdk = 21
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
    }
    buildFeatures {
        viewBinding = true
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
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-project.txt")
                )
            )
            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
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

    // sample mnemonic plugin
    implementation(libs.zcashwalletplgn)
    implementation(libs.bip39)

    // Android
    implementation(libs.androidx.core)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.material)
    androidTestImplementation(libs.bundles.androidx.test)

    implementation(libs.bundles.grpc)
}

fladle {
    // Firebase Test Lab has min and max values that might differ from our project's
    // These are determined by `gcloud firebase test android models list`
    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MIN_API = 19

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
                mapOf("model" to "Nexus5", "version" to minSdkVersion),
                mapOf("model" to "Pixel2.arm", "version" to targetSdkVersion)
            )

            flankVersion.set(libs.versions.flank.get())
        }
    }
}