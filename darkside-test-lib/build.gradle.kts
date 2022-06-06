plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")
    id("kotlin-kapt")
}

android {
    defaultConfig {
        //targetSdk = 30 //Integer.parseInt(project.property("targetSdkVersion"))
        multiDexEnabled = true
    }
}

dependencies {
    implementation(projects.sdkLib)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.multidex)
    implementation(libs.bundles.grpc)

    androidTestImplementation(libs.bundles.androidx.test)

    androidTestImplementation(libs.zcashwalletplgn)
    androidTestImplementation(libs.bip39)
}
