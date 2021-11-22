import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.android.library")
    id("zcash.android-build-conventions")
    kotlin("android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.dokka")
    id("com.google.protobuf")
    id("org.mozilla.rust-android-gradle.rust-android")
    id("com.vanniktech.maven.publish")
}

// Publishing information
val ARTIFACT_ID = project.property("POM_ARTIFACT_ID").toString()
project.group = "cash.z.ecc.android"
project.version = project.property("LIBRARY_VERSION").toString()
publishing {
    publications {
        publications.withType<MavenPublication>().all {
            artifactId = ARTIFACT_ID
        }
    }
}

android {
    useLibrary("android.test.runner")

    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
            }
        }

        consumerProguardFiles("proguard-consumer.txt")
    }

    buildTypes {
        getByName("debug").apply {
            // test builds exceed the dex limit because they pull in large test libraries
            multiDexEnabled = true
            isMinifyEnabled = false
        }
        getByName("release").apply {
            multiDexEnabled = false
            isMinifyEnabled = project.property("IS_MINIFY_SDK_ENABLED").toString().toBoolean()
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-project.txt")
                )
            )
        }
    }

    sourceSets.getByName("main") {
        java.srcDir("build/generated/source/grpc")
        proto { srcDir("src/main/proto") }
    }

    kotlinOptions {
        jvmTarget = libs.versions.java.get()
        allWarningsAsErrors = project.property("IS_TREAT_WARNINGS_AS_ERRORS").toString().toBoolean()
        freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.FlowPreview"
        // Tricky: fix: By default, the kotlin_module name will not include the version (in classes.jar/META-INF). Instead it has a colon, which breaks compilation on Windows. This is one way to set it explicitly to the proper value. See https://github.com/zcash/zcash-android-wallet-sdk/issues/222 for more info.
        freeCompilerArgs += listOf("-module-name", "$ARTIFACT_ID-${project.version}_release")
    }

    packagingOptions {
        resources.excludes.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        )
    }

    lint {
        baseline(File("lint-baseline.xml"))
    }
}

mavenPublish {
    androidVariantToPublish = "release"
}

allOpen {
    // marker for classes that we want to be able to extend in debug builds for testing purposes
    annotation("cash.z.ecc.android.sdk.annotation.OpenClass")
}

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        configureEach {
            outputDirectory.set(file("build/docs/rtd"))
            displayName.set("Zcash Android SDK")
            includes.from("packages.md")
        }
    }
}

protobuf {
    //generatedFilesBaseDir = "$projectDir/src/generated/source/grpc"
    protoc { artifact = libs.protoc.get().asCoordinateString() }
    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.get().asCoordinateString()
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
            task.plugins {
                id("grpc") {
                    option("lite")
                }
            }
        }
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
    apiLevels = mapOf(
        "arm" to 16,
        "arm64" to 21,
        "x86" to 16,
        "x86_64" to 21,
    )

    profile = "release"
    prebuiltToolchains = true
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)

    // Architecture Components: Lifecycle
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common)

    // Architecture Components: Room
    implementation(libs.androidx.room.core)
    implementation(libs.androidx.paging)
    kapt(libs.androidx.room.compiler)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // grpc-java
    implementation(libs.bundles.grpc)
    compileOnly(libs.javax.annotation)


    //
    // Locked Versions
    //    these should be checked regularly and removed when possible

    // solves error: Duplicate class com.google.common.util.concurrent.ListenableFuture found in modules jetified-guava-26.0-android.jar (com.google.guava:guava:26.0-android) and listenablefuture-1.0.jar (com.google.guava:listenablefuture:1.0)
    // per this recommendation from Chris Povirk, given guava's decision to split ListenableFuture away from Guava: https://groups.google.com/d/msg/guava-discuss/GghaKwusjcY/bCIAKfzOEwAJ
    implementation(libs.guava)
    // Transitive dependencies used because they're already necessary for other libraries
    // GSON is available as a transitive dependency from several places so we use it for processing
    // checkpoints but also document that by explicitly including here. If dependencies like Room
    // or grpc-okhttp stop including GSON, then we should switch to something else for parsing JSON.
    implementation(libs.gson)
    // OKIO is a transitive dependency used when writing param files to disk. Like GSON, this can be
    // replaced if needed. For compatibility, we match the library version used in grpc-okhttp:
    // https://github.com/grpc/grpc-java/blob/v1.37.x/build.gradle#L159
    implementation(libs.okio)

    // Tests
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.mockito.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.grpc.testing)

    // NOTE: androidTests will use JUnit4, while src/test/java tests will leverage Junit5
    // Attempting to use JUnit5 via https://github.com/mannodermaus/android-junit5 was painful. The plugin configuration
    // was buggy, crashing in several places. It also would require a separate test flavor because it's minimum API 26
    // because "JUnit 5 uses Java 8-specific APIs that didn't exist on Android before the Oreo release."
    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.coroutines.okhttp)
    // used by 'ru.gildor.corutines.okhttp.await' (to make simple suspended requests) and breaks on versions higher than 3.8.0
    androidTestImplementation(libs.okhttp)

    // sample mnemonic plugin
    androidTestImplementation(libs.zcashwalletplgn)
    androidTestImplementation(libs.bip39)
}

tasks {
    getByName("preBuild").dependsOn(create("bugfixTask") {
        doFirst {
            mkdir("build/extracted-include-protos/main")
        }
    })

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