plugins {
    id("com.android.application")
    id("zcash.android-build-conventions")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs")
}

android {
    defaultConfig {
        applicationId = "cash.z.ecc.android.sdk.demoapp"
        minSdk = 21 // Different from the SDK min
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        viewBinding = true
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
        }
    }

    kotlinOptions {
        jvmTarget = libs.versions.java.get()
        allWarningsAsErrors = project.property("IS_TREAT_WARNINGS_AS_ERRORS").toString().toBoolean()
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
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.material)
    androidTestImplementation(libs.bundles.androidx.test)

    implementation(libs.bundles.grpc)
}
