package cash.z.ecc.android

object Deps {
    // For use in the top-level build.gradle which gives an error when provided
    // `Deps.Kotlin.version` directly
    const val kotlinVersion =   "1.4.10"
    const val group =           "cash.z.ecc.android"
    const val artifactName =    "zcash-android-sdk"
    const val versionName =     "1.1.0-beta10"
    const val versionCode =     1_01_00_210  // last digits are alpha(0XX) beta(2XX) rc(4XX) release(8XX). Ex: 1_08_04_401 is an release candidate build of version 1.8.4 and 1_08_04_800 would be the final release.
    const val description =     "This lightweight SDK connects Android to Zcash. It welds together Rust and Kotlin in a minimal way, allowing third-party Android apps to send and receive shielded transactions easily, securely and privately."
    const val githubUrl =       "https://github.com/zcash/zcash-android-wallet-sdk"

    // publishing
    //      NOTE: to upload run: ./gradlew bintrayUpload after setting BINTRAY_USER and BINTRAY_API_KEY as environment variable
    //            to publish for local development run: ./gradlew publishToMavenLocal
    //      Remember: publish both mainnet and testnet!
    const val publishingDryRun = true
    val publishingTarget = Publication.Mainnet

    object Publication {
        object Mainnet {
            const val variant = "zcashmainnetRelease"
            const val artifactId = "zcash-android-sdk-mainnet"
        }
        object Testnet {
            const val variant = "zcashtestnetRelease"
            const val artifactId = "zcash-android-sdk-testnet"
        }
    }

    object Kotlin : Version(kotlinVersion) {
        val STDLIB =            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
    }
}

open class Version(@JvmField val version: String)
