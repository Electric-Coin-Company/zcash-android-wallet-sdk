plugins {
    id("com.android.test")
    kotlin("android")
    id("zcash-sdk.android-conventions")
}

android {
    namespace = "cash.z.ecc.android.sdk.demoapp.benchmark"
    targetProjectPath = ":${projects.demoApp.name}"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // To enable benchmarking for emulators, although only a physical device us gives real results
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        // To simplify module variants, we assume to run benchmarking against mainnet only
        missingDimensionStrategy("network", "zcashmainnet")
    }

    buildTypes {
        create("release") {
            // To provide compatibility with other modules
        }
        create("benchmark") {
            // We provide the extra benchmark build type for benchmarking. We still need to support debug
            // variants to be compatible with debug variants in other modules, although benchmarking does not allow
            // not minified build variants - benchmarking with the debug build variants will fail.
            isMinifyEnabled = true
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(libs.bundles.androidx.test)
    implementation(libs.androidx.test.macrobenchmark)
    implementation(libs.androidx.uiAutomator)

    if (project.property("IS_USE_TEST_ORCHESTRATOR").toString().toBoolean()) {
        implementation(libs.androidx.test.orchestrator) {
            artifact {
                type = "apk"
            }
        }
    }
}