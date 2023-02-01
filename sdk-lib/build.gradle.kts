import java.util.Base64
import java.util.Locale

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")

    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.dokka")
    id("org.mozilla.rust-android-gradle.rust-android")

    id("wtf.emulator.gradle")
    id("zcash-sdk.emulator-wtf-conventions")

    id("maven-publish")
    id("signing")
}

// Publishing information
val publicationVariant = "release"
val myVersion = project.property("LIBRARY_VERSION").toString()
val myArtifactId = "zcash-android-sdk"
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
                name.set("Zcash Android Wallet SDK")
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
    namespace = "cash.z.ecc.android.sdk"

    useLibrary("android.test.runner")

    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
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
        create("benchmark") {
            // We provide the extra benchmark build type just for benchmarking purposes
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    kotlinOptions {
        // Tricky: fix: By default, the kotlin_module name will not include the version (in classes.jar/META-INF).
        // Instead it has a colon, which breaks compilation on Windows. This is one way to set it explicitly to the
        // proper value. See https://github.com/zcash/zcash-android-wallet-sdk/issues/222 for more info.
        freeCompilerArgs += listOf("-module-name", "$myArtifactId-${myVersion}_release")
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

androidComponents {
    onVariants { variant ->
        if (variant.name.toLowerCase(Locale.US).contains("release")) {
            variant.packaging.resources.excludes.addAll(
                listOf(
                    "META-INF/ASL2.0",
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE-notice.md",
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/license.txt",
                    "META-INF/notice.txt"
                )
            )
        }
    }
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
    api(projects.lightwalletClientLib)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)

    // Architecture Components: Lifecycle
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common)

    // Architecture Components: Room
    implementation(libs.androidx.room.core)
    implementation(libs.androidx.paging)
    ksp(libs.androidx.room.compiler)

    // For direct database access
    // TODO [#703]: Eliminate this dependency
    // https://github.com/zcash/zcash-android-wallet-sdk/issues/703
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.framework)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.junit)

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
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // sample mnemonic plugin
    androidTestImplementation(libs.zcashwalletplgn)
    androidTestImplementation(libs.bip39)
}

tasks {
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