enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        val isRepoRestrictionEnabled = true
        mavenCentral {
            if (isRepoRestrictionEnabled) {
                content {
                    includeGroup("wtf.emulator")
                }
            }
        }
        gradlePluginPortal()
        google()
    }

    plugins {
        val androidGradlePluginVersion = extra["ANDROID_GRADLE_PLUGIN_VERSION"].toString()
        val detektVersion = extra["DETEKT_VERSION"].toString()
        val dokkaVersion = extra["DOKKA_VERSION"].toString()
        val emulatorWtfGradlePluginVersion = extra["EMULATOR_WTF_GRADLE_PLUGIN_VERSION"].toString()
        val fulladleVersion = extra["FULLADLE_VERSION"].toString()
        val gradleVersionsPluginVersion = extra["GRADLE_VERSIONS_PLUGIN_VERSION"].toString()
        val kotlinVersion = extra["KOTLIN_VERSION"].toString()
        val kspVersion = extra["KSP_VERSION"].toString()
        val mavenPublishPluginVersion = extra["MAVEN_PUBLISH_GRADLE_PLUGIN"].toString()
        val protobufVersion = extra["PROTOBUF_GRADLE_PLUGIN_VERSION"].toString()

        id("com.android.application") version (androidGradlePluginVersion) apply (false)
        id("com.android.library") version (androidGradlePluginVersion) apply (false)
        id("com.github.ben-manes.versions") version (gradleVersionsPluginVersion) apply (false)
        id("com.google.devtools.ksp") version(kspVersion) apply (false)
        id("com.google.protobuf") version (protobufVersion) apply (false)
        id("com.osacky.fulladle") version (fulladleVersion) apply (false)
        id("com.vanniktech.maven.publish.base") version(mavenPublishPluginVersion) apply (false)
        id("io.gitlab.arturbosch.detekt") version (detektVersion) apply (false)
        id("org.jetbrains.dokka") version (dokkaVersion) apply (false)
        id("org.jetbrains.kotlin.android") version (kotlinVersion) apply (false)
        id("org.jetbrains.kotlin.plugin.allopen") version (kotlinVersion) apply (false)
        id("wtf.emulator.gradle") version (emulatorWtfGradlePluginVersion) apply (false)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        val isRepoRestrictionEnabled = true

        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
        maven("https://maven.emulator.wtf/releases/") {
            if (isRepoRestrictionEnabled) {
                content {
                    includeGroup("wtf.emulator")
                }
            }
        }
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
            val flankVersion = extra["FLANK_VERSION"].toString()
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
            version("flank", flankVersion)
            version("grpc", grpcVersion)
            version("java", javaVersion)
            version("kotlin", kotlinVersion)
            version("protoc", protocVersion)

            // Aliases
            // Gradle plugins
            library("gradle-plugin-android", "com.android.tools.build:gradle:$androidGradlePluginVersion")
            library("gradle-plugin-navigation", "androidx.navigation:navigation-safe-args-gradle-plugin:$androidxNavigationVersion")
            library("gradle-plugin-publish", "com.vanniktech:gradle-maven-publish-plugin:$mavenPublishGradlePluginVersion")
            library("gradle-plugin-rust", "org.mozilla.rust-android-gradle:plugin:$rustGradlePluginVersion")

            // Special cases used by the grpc gradle plugin
            library("grpc-protoc", "io.grpc:protoc-gen-grpc-java:$grpcVersion")
            library("protoc", "com.google.protobuf:protoc:$protocVersion")

            // Libraries
            library("androidx-annotation", "androidx.annotation:annotation:$androidxAnnotationVersion")
            library("androidx-appcompat", "androidx.appcompat:appcompat:$androidxAppcompatVersion")
            library("androidx-constraintlayout", "androidx.constraintlayout:constraintlayout:$androidxConstraintLayoutVersion")
            library("androidx-core", "androidx.core:core-ktx:$androidxCoreVersion")
            library("androidx-lifecycle-common", "androidx.lifecycle:lifecycle-common-java8:$androidxLifecycleVersion")
            library("androidx-lifecycle-runtime", "androidx.lifecycle:lifecycle-runtime-ktx:$androidxLifecycleVersion")
            library("androidx-multidex", "androidx.multidex:multidex:$androidxMultidexVersion")
            library("androidx-navigation-fragment", "androidx.navigation:navigation-fragment-ktx:$androidxNavigationVersion")
            library("androidx-navigation-ui", "androidx.navigation:navigation-ui-ktx:$androidxNavigationVersion")
            library("androidx-paging", "androidx.paging:paging-runtime-ktx:$androidxPagingVersion")
            library("androidx-room-compiler", "androidx.room:room-compiler:$androidxRoomVersion")
            library("androidx-room-core", "androidx.room:room-ktx:$androidxRoomVersion")
            library("bip39", "cash.z.ecc.android:kotlin-bip39:$bip39Version")
            library("grpc-android", "io.grpc:grpc-android:$grpcVersion")
            library("grpc-okhttp", "io.grpc:grpc-okhttp:$grpcVersion")
            library("grpc-protobuf", "io.grpc:grpc-protobuf-lite:$grpcVersion")
            library("grpc-stub", "io.grpc:grpc-stub:$grpcVersion")
            library("gson", "com.google.code.gson:gson:$gsonVersion")
            library("guava", "com.google.guava:guava:$guavaVersion")
            library("javax-annotation", "javax.annotation:javax.annotation-api:$javaxAnnotationVersion")
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            library("kotlinx-coroutines-android", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
            library("material", "com.google.android.material:material:$googleMaterialVersion")
            library("okhttp", "com.squareup.okhttp3:okhttp:$okhttpVersion")
            library("okio", "com.squareup.okio:okio:$okioVersion")
            library("zcashwalletplgn", "com.github.zcash:zcash-android-wallet-plugins:$zcashWalletPluginVersion")

            // Test libraries
            library("androidx-espresso-contrib", "androidx.test.espresso:espresso-contrib:$androidxEspressoVersion")
            library("androidx-espresso-core", "androidx.test.espresso:espresso-core:$androidxEspressoVersion")
            library("androidx-espresso-intents", "androidx.test.espresso:espresso-intents:$androidxEspressoVersion")
            library("androidx-test-core", "androidx.test:core:$androidxTestVersion")
            library("androidx-test-junit", "androidx.test.ext:junit:$androidxTestJunitVersion")
            library("androidx-test-runner", "androidx.test:runner:$androidxTestVersion")
            library("androidx-testOrchestrator", "androidx.test:orchestrator:$androidxTestOrchestratorVersion")
            library("androidx-uiAutomator", "androidx.test.uiautomator:uiautomator-v18:$androidxUiAutomatorVersion")
            library("coroutines-okhttp", "ru.gildor.coroutines:kotlin-coroutines-okhttp:$coroutinesOkhttpVersion")
            library("grpc-testing", "io.grpc:grpc-testing:$grpcVersion")
            library("junit-api", "org.junit.jupiter:junit-jupiter-api:$junitVersion")
            library("junit-engine", "org.junit.jupiter:junit-jupiter-engine:$junitVersion")
            library("junit-migration", "org.junit.jupiter:junit-jupiter-migrationsupport:$junitVersion")
            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
            library("mockito-android", "org.mockito:mockito-android:$mockitoVersion")
            library("mockito-junit", "org.mockito:mockito-junit-jupiter:$mockitoVersion")
            library("mockito-kotlin", "com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")

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