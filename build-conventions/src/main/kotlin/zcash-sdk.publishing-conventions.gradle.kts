import java.util.Base64

val publicationVariant = "release"
val isSnapshot = project.property("IS_SNAPSHOT").toString().toBoolean()
val myVersion = project.property("LIBRARY_VERSION").toString()

val myGroup = "cash.z.ecc.android"
project.group = myGroup

pluginManager.withPlugin("com.android.library") {
    project.the<com.android.build.gradle.LibraryExtension>().apply {
        publishing {
            singleVariant(publicationVariant) {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
}

plugins.withId("org.gradle.maven-publish") {
    val publishingExtension = extensions.getByType<PublishingExtension>().apply {
        publications {
            register<MavenPublication>("release") {
                groupId = myGroup
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
                    description.set(
                        "This lightweight SDK connects Android to Zcash, allowing third-party " +
                            "Android apps to send and receive shielded transactions easily, securely and privately."
                    )
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

    plugins.withId("org.gradle.signing") {
        project.the<SigningExtension>().apply {
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

            sign(publishingExtension.publications)
        }
    }
}
