enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }

    plugins {
        val androidGradlePluginVersion = extra["ANDROID_GRADLE_PLUGIN_VERSION"].toString()
        val detektVersion = extra["DETEKT_VERSION"].toString()
        val dokkaVersion = extra["DOKKA_VERSION"].toString()
        val gradleVersionsPluginVersion = extra["GRADLE_VERSIONS_PLUGIN_VERSION"].toString()
        val kotlinVersion = extra["KOTLIN_VERSION"].toString()
        val kspVersion = extra["KSP_VERSION"].toString()
        val owaspVersion = extra["OWASP_DEPENDENCY_CHECK_VERSION"].toString()
        val protobufVersion = extra["PROTOBUF_GRADLE_PLUGIN_VERSION"].toString()

        id("com.android.application") version (androidGradlePluginVersion) apply (false)
        id("com.android.library") version (androidGradlePluginVersion) apply (false)
        id("com.github.ben-manes.versions") version (gradleVersionsPluginVersion) apply (false)
        id("com.google.devtools.ksp") version(kspVersion) apply (false)
        id("com.google.protobuf") version (protobufVersion) apply (false)
        id("io.gitlab.arturbosch.detekt") version (detektVersion) apply (false)
        id("org.jetbrains.dokka") version (dokkaVersion) apply (false)
        id("org.jetbrains.kotlin.android") version (kotlinVersion) apply (false)
        id("org.jetbrains.kotlin.plugin.allopen") version (kotlinVersion) apply (false)
        id("org.owasp.dependencycheck") version (owaspVersion) apply (false)
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }

    @Suppress("UnstableApiUsage", "MaxLineLength")
    versionCatalogs {
        create("libs") {
            val androidGradlePluginVersion = extra["ANDROID_GRADLE_PLUGIN_VERSION"].toString()
            val androidxAnnotationVersion = extra["ANDROIDX_ANNOTATION_VERSION"].toString()
            val androidxAppcompatVersion = extra["ANDROIDX_APPCOMPAT_VERSION"].toString()
            val androidxConstraintLayoutVersion = extra["ANDROIDX_CONSTRAINT_LAYOUT_VERSION"].toString()
            val androidxCoreVersion = extra["ANDROIDX_CORE_VERSION"].toString()
            val androidxEspressoVersion = extra["ANDROIDX_ESPRESSO_VERSION"].toString()
            val androidxLifecycleVersion = extra["ANDROIDX_LIFECYCLE_VERSION"].toString()
            val androidxMultidexVersion = extra["ANDROIDX_MULTIDEX_VERSION"].toString()
            val androidxNavigationVersion = extra["ANDROIDX_NAVIGATION_VERSION"].toString()
            val androidxPagingVersion = extra["ANDROIDX_PAGING_VERSION"].toString()
            val androidxRoomVersion = extra["ANDROIDX_ROOM_VERSION"].toString()
            val androidxTestJunitVersion = extra["ANDROIDX_TEST_JUNIT_VERSION"].toString()
            val androidxTestOrchestratorVersion = extra["ANDROIDX_ESPRESSO_VERSION"].toString()
            val androidxTestVersion = extra["ANDROIDX_TEST_VERSION"].toString()
            val androidxUiAutomatorVersion = extra["ANDROIDX_UI_AUTOMATOR_VERSION"].toString()
            val bip39Version = extra["BIP39_VERSION"].toString()
            val coroutinesOkhttpVersion = extra["COROUTINES_OKHTTP"].toString()
            val googleMaterialVersion = extra["GOOGLE_MATERIAL_VERSION"].toString()
            val grpcVersion = extra["GRPC_VERSION"].toString()
            val gsonVersion = extra["GSON_VERSION"].toString()
            val guavaVersion = extra["GUAVA_VERSION"].toString()
            val javaVersion = extra["ANDROID_JVM_TARGET"].toString()
            val javaxAnnotationVersion = extra["JAVAX_ANNOTATION_VERSION"].toString()
            val junitVersion = extra["JUNIT_VERSION"].toString()
            val kotlinVersion = extra["KOTLIN_VERSION"].toString()
            val kotlinxCoroutinesVersion = extra["KOTLINX_COROUTINES_VERSION"].toString()
            val mavenPublishGradlePluginVersion = extra["MAVEN_PUBLISH_GRADLE_PLUGIN"].toString()
            val mockitoKotlinVersion = extra["MOCKITO_KOTLIN_VERSION"].toString()
            val mockitoVersion = extra["MOCKITO_VERSION"].toString()
            val okhttpVersion = extra["OKHTTP_VERSION"].toString()
            val okioVersion = extra["OKIO_VERSION"].toString()
            val protocVersion = extra["PROTOC_VERSION"].toString()
            val rustGradlePluginVersion = extra["RUST_GRADLE_PLUGIN_VERSION"].toString()
            val zcashWalletPluginVersion = extra["ZCASH_WALLET_PLUGINS_VERSION"].toString()

            // Standalone versions
            version("grpc", grpcVersion)
            version("java", javaVersion)
            version("kotlin", kotlinVersion)
            version("protoc", protocVersion)

            // Aliases
            // Gradle plugins
            alias("gradle-plugin-android").to("com.android.tools.build:gradle:$androidGradlePluginVersion")
            alias("gradle-plugin-navigation").to("androidx.navigation:navigation-safe-args-gradle-plugin:$androidxNavigationVersion")
            alias("gradle-plugin-publish").to("com.vanniktech:gradle-maven-publish-plugin:$mavenPublishGradlePluginVersion")
            alias("gradle-plugin-rust").to("org.mozilla.rust-android-gradle:plugin:$rustGradlePluginVersion")

            // Special cases used by the grpc gradle plugin
            alias("grpc-protoc").to("io.grpc:protoc-gen-grpc-java:$grpcVersion")
            alias("protoc").to("com.google.protobuf:protoc:$protocVersion")

            // Libraries
            alias("androidx-annotation").to("androidx.annotation:annotation:$androidxAnnotationVersion")
            alias("androidx-appcompat").to("androidx.appcompat:appcompat:$androidxAppcompatVersion")
            alias("androidx-constraintlayout").to("androidx.constraintlayout:constraintlayout:$androidxConstraintLayoutVersion")
            alias("androidx-core").to("androidx.core:core-ktx:$androidxCoreVersion")
            alias("androidx-lifecycle-common").to("androidx.lifecycle:lifecycle-common-java8:$androidxLifecycleVersion")
            alias("androidx-lifecycle-runtime").to("androidx.lifecycle:lifecycle-runtime-ktx:$androidxLifecycleVersion")
            alias("androidx-multidex").to("androidx.multidex:multidex:$androidxMultidexVersion")
            alias("androidx-navigation-fragment").to("androidx.navigation:navigation-fragment-ktx:$androidxNavigationVersion")
            alias("androidx-navigation-ui").to("androidx.navigation:navigation-ui-ktx:$androidxNavigationVersion")
            alias("androidx-paging").to("androidx.paging:paging-runtime-ktx:$androidxPagingVersion")
            alias("androidx-room-compiler").to("androidx.room:room-compiler:$androidxRoomVersion")
            alias("androidx-room-core").to("androidx.room:room-ktx:$androidxRoomVersion")
            alias("bip39").to("cash.z.ecc.android:kotlin-bip39:$bip39Version")
            alias("grpc-android").to("io.grpc:grpc-android:$grpcVersion")
            alias("grpc-okhttp").to("io.grpc:grpc-okhttp:$grpcVersion")
            alias("grpc-protobuf").to("io.grpc:grpc-protobuf-lite:$grpcVersion")
            alias("grpc-stub").to("io.grpc:grpc-stub:$grpcVersion")
            alias("gson").to("com.google.code.gson:gson:$gsonVersion")
            alias("guava").to("com.google.guava:guava:$guavaVersion")
            alias("javax-annotation").to("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")
            alias("kotlin-reflect").to("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            alias("kotlin-stdlib").to("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            alias("kotlinx-coroutines-android").to("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")
            alias("kotlinx-coroutines-core").to("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
            alias("material").to("com.google.android.material:material:$googleMaterialVersion")
            alias("okhttp").to("com.squareup.okhttp3:okhttp:$okhttpVersion")
            alias("okio").to("com.squareup.okio:okio:$okioVersion")
            alias("zcashwalletplgn").to("com.github.zcash:zcash-android-wallet-plugins:$zcashWalletPluginVersion")

            // Test libraries
            alias("androidx-espresso-contrib").to("androidx.test.espresso:espresso-contrib:$androidxEspressoVersion")
            alias("androidx-espresso-core").to("androidx.test.espresso:espresso-core:$androidxEspressoVersion")
            alias("androidx-espresso-intents").to("androidx.test.espresso:espresso-intents:$androidxEspressoVersion")
            alias("androidx-test-core").to("androidx.test:core:$androidxTestVersion")
            alias("androidx-test-junit").to("androidx.test.ext:junit:$androidxTestJunitVersion")
            alias("androidx-test-runner").to("androidx.test:runner:$androidxTestVersion")
            alias("androidx-testOrchestrator").to("androidx.test:orchestrator:$androidxTestOrchestratorVersion")
            alias("androidx-uiAutomator").to("androidx.test.uiautomator:uiautomator-v18:$androidxUiAutomatorVersion")
            alias("coroutines-okhttp").to("ru.gildor.coroutines:kotlin-coroutines-okhttp:$coroutinesOkhttpVersion")
            alias("grpc-testing").to("io.grpc:grpc-testing:$grpcVersion")
            alias("junit-api").to("org.junit.jupiter:junit-jupiter-api:$junitVersion")
            alias("junit-engine").to("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
            alias("junit-migration").to("org.junit.jupiter:junit-jupiter-migrationsupport:$junitVersion")
            alias("kotlinx-coroutines-test").to("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
            alias("mockito-android").to("org.mockito:mockito-android:$mockitoVersion")
            alias("mockito-junit").to("org.mockito:mockito-junit-jupiter:$mockitoVersion")
            alias("mockito-kotlin").to("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")

            // Bundles
            bundle(
                "androidx-test",
                listOf(
                    "androidx-espresso-core",
                    "androidx-espresso-intents",
                    "androidx-test-junit",
                    "androidx-test-core"
                )
            )

            bundle(
                "grpc",
                listOf(
                    "grpc-okhttp",
                    "grpc-android",
                    "grpc-protobuf",
                    "grpc-stub"
                )
            )

            bundle(
                "junit",
                listOf(
                    "junit-api",
                    "junit-engine",
                    "junit-migration",
                )
            )
        }
    }
}

rootProject.name = "zcash-android-sdk"

includeBuild("build-conventions")

include("darkside-test-lib")
include("sdk-lib")
include("demo-app")