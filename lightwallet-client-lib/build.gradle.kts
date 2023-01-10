import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

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
}

// Publishing information
val publicationVariant = "release"
val myVersion = project.property("LIBRARY_VERSION").toString()
val myArtifactId = "client"
val isSnapshot = project.property("IS_SNAPSHOT").toString().toBoolean()
project.group = "co.electriccoin.lightwallet"

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = myArtifactId
            version = if (isSnapshot) {
                "$myVersion-SNAPSHOT"
            } else {
                myVersion
            }

            afterEvaluate {
                from(components[publicationVariant])
            }

            pom {
                name.set("Zcash Light Wallet Client")
                description.set("Client API for connecting to the Light Wallet server.")
                url.set("https://github.com/zcash/zcash-android-wallet-sdk/")
                inceptionYear.set("2022")
                scm {
                    url.set("https://github.com/zcash/zcash-android-wallet-sdk/")
                    connection.set("scm:git:git://github.com/zcash/zcash-android-wallet-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/zcash/zcash-android-wallet-sdk.git")
                }
                developers {
                    developer {
                        id.set("zcash")
                        name.set("Zcash")
                        url.set("https://github.com/zcash/")
                    }
                }
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("http://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
            }
        }
    }
    repositories {
        val mavenUrl = if (isSnapshot) {
            project.property("ZCASH_MAVEN_PUBLISH_SNAPSHOT_URL").toString()
        } else {
            project.property("ZCASH_MAVEN_PUBLISH_RELEASE_URL").toString()
        }
        val mavenPublishUsername = project.property("ZCASH_MAVEN_PUBLISH_USERNAME").toString()
        val mavenPublishPassword = project.property("ZCASH_MAVEN_PUBLISH_PASSWORD").toString()

        mavenLocal {
            name = "MavenLocal"
        }
        maven(mavenUrl) {
            name = "MavenCentral"
            credentials {
                username = mavenPublishUsername
                password = mavenPublishPassword
            }
        }
    }
}

android {
    useLibrary("android.test.runner")

    defaultConfig {
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
        create("benchmark") {
            // We provide the extra benchmark build type just for benchmarking purposes
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    sourceSets.getByName("main") {
        proto { srcDir("src/main/proto") }
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
        baseline = File("lint-baseline.xml")
    }

    publishing {
        singleVariant(publicationVariant) {
            withSourcesJar()
            withJavadocJar()
        }
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
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // TODO [#673]: Make `implementation` https://github.com/zcash/zcash-android-wallet-sdk/issues/673
    api(libs.bundles.grpc)

    // Tests
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.grpc.testing)

    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.coroutines.okhttp)
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
