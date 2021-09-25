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
    }

    buildTypes {
        getByName("debug").apply {
            // test builds exceed the dex limit because they pull in large test libraries
            multiDexEnabled = true
            isMinifyEnabled = false
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-rules.pro")
                )
            )
        }
        getByName("release").apply {
            multiDexEnabled = false
            isMinifyEnabled = project.property("IS_MINIFY_ENABLED").toString().toBoolean()
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-rules.pro")
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
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}" }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
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
    profile = "release"
    prebuiltToolchains = true
}

dependencies {
    implementation(AndroidX.appCompat)

    // Architecture Components: Lifecycle
    implementation(AndroidX.lifecycle.runtimeKtx)
    implementation(AndroidX.lifecycle.commonJava8)

    // Architecture Components: Room
    implementation(AndroidX.room.ktx)
    implementation(AndroidX.paging.runtimeKtx)
    kapt(AndroidX.room.compiler)

    // Kotlin
    implementation(Kotlin.stdlib.jdk8)
    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.android)

    // grpc-java
    implementation("io.grpc:grpc-okhttp:${libs.versions.grpc.get()}")
    implementation("io.grpc:grpc-android:${libs.versions.grpc.get()}")
    implementation("io.grpc:grpc-protobuf-lite:${libs.versions.grpc.get()}")
    implementation("io.grpc:grpc-stub:${libs.versions.grpc.get()}")
    compileOnly("javax.annotation:javax.annotation-api:_")


    //
    // Locked Versions
    //    these should be checked regularly and removed when possible

    // solves error: Duplicate class com.google.common.util.concurrent.ListenableFuture found in modules jetified-guava-26.0-android.jar (com.google.guava:guava:26.0-android) and listenablefuture-1.0.jar (com.google.guava:listenablefuture:1.0)
    // per this recommendation from Chris Povirk, given guava's decision to split ListenableFuture away from Guava: https://groups.google.com/d/msg/guava-discuss/GghaKwusjcY/bCIAKfzOEwAJ
    implementation("com.google.guava:guava:30.0-android")
    // Transitive dependencies used because they're already necessary for other libraries
    // GSON is available as a transitive dependency from several places so we use it for processing
    // checkpoints but also document that by explicitly including here. If dependencies like Room
    // or grpc-okhttp stop including GSON, then we should switch to something else for parsing JSON.
    implementation("com.google.code.gson:gson:2.8.6")
    // OKIO is a transitive dependency used when writing param files to disk. Like GSON, this can be
    // replaced if needed. For compatibility, we match the library version used in grpc-okhttp:
    // https://github.com/grpc/grpc-java/blob/v1.37.x/build.gradle#L159
    implementation("com.squareup.okio:okio:1.17.5")

    // Tests
    testImplementation("androidx.multidex:multidex:_")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:_")
    testImplementation("org.mockito:mockito-junit-jupiter:_")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:_")
    testImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:_")
    testImplementation("org.junit.jupiter:junit-jupiter-migrationsupport:_")
    testImplementation("io.grpc:grpc-testing:${libs.versions.grpc.get()}")

    // NOTE: androidTests will use JUnit4, while src/test/java tests will leverage Junit5
    // Attempting to use JUnit5 via https://github.com/mannodermaus/android-junit5 was painful. The plugin configuration
    // was buggy, crashing in several places. It also would require a separate test flavor because it's minimum API 26
    // because "JUnit 5 uses Java 8-specific APIs that didn't exist on Android before the Oreo release."
    androidTestImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:_")
    androidTestImplementation("org.mockito:mockito-android:_")
    androidTestImplementation("androidx.test:runner:_")
    androidTestImplementation("com.android.support:support-annotations:_")
    androidTestImplementation("androidx.test:core:_")
    androidTestImplementation("androidx.arch.core:core-testing:_")
    androidTestImplementation("androidx.test.ext:junit:_")
    androidTestImplementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:_")
    // used by 'ru.gildor.corutines.okhttp.await' (to make simple suspended requests) and breaks on versions higher than 3.8.0
    androidTestImplementation("com.squareup.okhttp3:okhttp:3.8.0")

    // sample mnemonic plugin
    androidTestImplementation("com.github.zcash:zcash-android-wallet-plugins:_")
    androidTestImplementation("cash.z.ecc.android:kotlin-bip39:_")
}

tasks.getByName("preBuild").dependsOn(tasks.create("bugfixTask") {
    doFirst {
        mkdir("build/extracted-include-protos/main")
    }
})

project.afterEvaluate {
    val cargoTask = tasks.getByName("cargoBuild")
    tasks.getByName("javaPreCompileDebug").dependsOn(cargoTask)
    tasks.getByName("javaPreCompileRelease").dependsOn(cargoTask)
}