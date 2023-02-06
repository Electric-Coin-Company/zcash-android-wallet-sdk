import java.util.Base64

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")

    id("org.jetbrains.dokka")

    id("wtf.emulator.gradle")
    id("zcash-sdk.emulator-wtf-conventions")

    id("maven-publish")
    id("signing")
}

// Publishing information
val publicationVariant = "release"
val myVersion = project.property("LIBRARY_VERSION").toString()
val myArtifactId = "zcash-android-sdk-incubator"
val isSnapshot = project.property("IS_SNAPSHOT").toString().toBoolean()
val version = project.property("LIBRARY_VERSION").toString()
project.group = "cash.z.ecc.android"

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = myArtifactId
            groupId = "cash.z.ecc.android"
            version = if (isSnapshot) {
                "$myVersion-SNAPSHOT"
            } else {
                myVersion
            }

            afterEvaluate {
                from(components[publicationVariant])
            }

            pom {
                name.set("Zcash Android Wallet SDK In")
                description.set("This lightweight SDK connects Android to Zcash, allowing third-party " +
                    "Android apps to send and receive shielded transactions easily, securely and privately.")
                url.set("https://github.com/zcash/zcash-android-wallet-sdk/")
                inceptionYear.set("2018")
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

signing {
    // Maven Central requires signing for non-snapshots
    isRequired = !isSnapshot

    val signingKey = run {
        val base64EncodedKey = project.property("ZCASH_ASCII_GPG_KEY").toString()
        if (base64EncodedKey.isNotEmpty()) {
            val keyBytes = Base64.getDecoder().decode(base64EncodedKey)
            String(keyBytes)
        } else {
            ""
        }
    }

    if (signingKey.isNotEmpty()) {
        useInMemoryPgpKeys(signingKey, "")
    }

    sign(publishing.publications)
}

android {
    namespace = "cash.z.ecc.android.sdk.incubator"

    useLibrary("android.test.runner")

    defaultConfig {
        consumerProguardFiles("proguard-consumer.txt")
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
            displayName.set("Zcash Android SDK")
            includes.from("packages.md")
        }
    }
}

dependencies {
    implementation(projects.sdkLib)
    implementation(libs.bip39)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)

    implementation(libs.kotlinx.datetime)

    // Architecture Components: Lifecycle
    // implementation(libs.androidx.lifecycle.runtime)
    // implementation(libs.androidx.lifecycle.common)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // sample mnemonic plugin
    androidTestImplementation(libs.zcashwalletplgn)
}
