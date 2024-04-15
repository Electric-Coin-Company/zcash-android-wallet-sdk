import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")

    id("org.jetbrains.dokka")
    id("com.google.protobuf")

    id("wtf.emulator.gradle")
    id("zcash-sdk.emulator-wtf-conventions")

    id("maven-publish")
    id("signing")
    id("zcash-sdk.publishing-conventions")
}

publishing {
    publications {
        publications.withType<MavenPublication>().all {
            artifactId = "lightwallet-client"
        }
    }
}

android {
    namespace = "co.electriccoin.lightwallet.client"
    useLibrary("android.test.runner")

    defaultConfig {
        consumerProguardFiles("proguard-consumer.txt")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug").apply {
            isMinifyEnabled = false
        }
        getByName("release").apply {
            isMinifyEnabled = project.property("IS_MINIFY_SDK_ENABLED").toString().toBoolean()
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-project.txt")
                )
            )
        }
        create("benchmark") {
            // We provide the extra benchmark build type just for benchmarking purposes
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    sourceSets.getByName("main") {
        proto { srcDir("src/main/proto") }
    }

    lint {
        baseline = File("lint-baseline.xml")
    }
}

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        configureEach {
            outputDirectory.set(file("build/docs/rtd"))
            displayName.set("Lightwallet Client")
            includes.from("packages.md")
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.compiler.get().asCoordinateString()
    }
    plugins {
        id("java") {
            artifact = libs.protoc.gen.java.get().asCoordinateString()
        }
        id("grpc") {
            artifact = libs.protoc.gen.java.get().asCoordinateString()
        }
        id("grpckt") {
            artifact = libs.protoc.gen.kotlin.get().asCoordinateString() + ":jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("java") {
                    option("lite")
                }
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
            it.builtins {
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.bundles.grpc)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.grpc.testing)

    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test)
}

tasks {
    getByName("preBuild").dependsOn(create("bugfixTask") {
        doFirst {
            mkdir("build/extracted-include-protos/main")
        }
    })
}

fun MinimalExternalModuleDependency.asCoordinateString() =
    "${module.group}:${module.name}:${versionConstraint.displayName}"
