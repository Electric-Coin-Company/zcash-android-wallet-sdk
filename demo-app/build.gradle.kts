plugins {
    id("com.android.application")
    id("zcash.android-build-conventions")
    id("kotlin-android")
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
            isMinifyEnabled = project.property("IS_MINIFY_ENABLED").toString().toBoolean()
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
        baseline(File("lint-baseline.xml"))
    }
}

dependencies {
    // SDK
    implementation(projects.sdkLib)
    //implementation('cash.z.ecc.android:zcash-android-sdk:1.3.0-beta17')

    // sample mnemonic plugin
    implementation("com.github.zcash:zcash-android-wallet-plugins:1.0.1")
    implementation("cash.z.ecc.android:kotlin-bip39:1.0.2")

    // Android
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.1")
    implementation("com.google.android.material:material:1.3.0-alpha03")
    testImplementation("junit:junit:4.13")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

    implementation("io.grpc:grpc-android:${libs.versions.grpc.get()}")
    implementation("io.grpc:grpc-okhttp:${libs.versions.grpc.get()}")
    implementation("io.grpc:grpc-protobuf-lite:${libs.versions.grpc.get()}")
    implementation("io.grpc:grpc-stub:${libs.versions.grpc.get()}")
}
