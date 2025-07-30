# Overview
We aim for the main branch of the repository to always be in a releasable state.

Two types of artifacts can be published:
1. Snapshot — An unstable release of the SDK for testing
1. Release — A stable release of the SDK

Control of these modes of release is managed with a Gradle property `IS_SNAPSHOT`.

For both snapshot and release publishing, there are two ways to initiate deployment:
1. Automatically
2. Manually

This document will focus initially on the automated process, with a section at the end on manual process.  (The automated process more or less implements the manual process via GitHub Actions.)

# Automated Publishing
## Snapshots
All merges to the main branch trigger an automated [snapshot deployment](https://github.com/zcash/zcash-android-wallet-sdk/actions/workflows/deploy-snapshot.yml).

Note that snapshots do not have a stable API, so clients should not depend on a snapshot.  The primary reason this is documented is for testing, e.g. before deploying a new production version of the SDK we may test against the snapshot first.

Snapshots can be consumed by:

1. Adding the snapshot repository
settings.gradle.kts:
```
dependencyResolutionManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            // Optional; ensures only explicitly declared dependencies come from this repository
            content {
                includeGroup("cash.z.ecc.android")
            }
        }
    }
}
```

2. Changing the dependency version to end with `-SNAPSHOT`

3. Rebuilding
`./gradlew assemble --refresh-dependencies`

Because Gradle caches dependencies and because multiple snapshots can be deployed under the same version number, using `--refresh-dependencies` is important to ensure the latest snapshot is pulled.  (#533 will make it easier to identify version of the snapshot in the future).

## Releases
Production releases can be consumed using the instructions in the [README.MD](../README.md).  Note that production releases can include alpha or beta designations.

Automated production releases require a manual trigger of the GitHub action and a manual step inside the Sonatype dashboard.  To do a production release:
1. Update the [CHANGELOG](../CHANGELOG.md) for any new changes since the last production release.
1. Run the [release deployment](https://github.com/zcash/zcash-android-wallet-sdk/actions/workflows/deploy-release.yml).
1. Log into Maven Central and release the deployment.
    1. Check the contents of the staging repository, to verify it looks correct
    1. Close the staging repository
    1. Wait a few minutes and refresh the page
    1. Release the staging repository
1. Confirm deployment succeeded by modifying the [Zashi Wallet](https://github.com/Electric-Coin-Company/zashi-android) to consume the new SDK version.
1. Create a new Git tag for the new release in this repository.
1. Create a new pull request bumping the version to the next version (this ensures that the next merge to the main branch creates a snapshot under the next version number).

# Manual Publishing
See [CI.md](CI.md), which describes the continuous integration workflow for deployment and describes the secrets that 
would need to be configured in a repository fork.

## One time only
* Set up environment to [compile the SDK](https://github.com/zcash/zcash-android-wallet-sdk/#compiling-sources)
* Create file `~/.gradle/gradle.properties`
  * add your sonotype credentials with these properties
      * `mavenCentralUsername`
      * `mavenCentralPassword`
  * Point it to a passwordless GPG key that has been ASCII armored, then base64 encoded.
     * `ZCASH_ASCII_GPG_KEY`

## Every time
1. Update the [build number](https://github.com/zcash/zcash-android-wallet-sdk/blob/main/gradle.properties) and the [CHANGELOG](../CHANGELOG.md).  For release builds, suffix the Gradle invocations below with `-PIS_SNAPSHOT=false`.
3. Build locally
    * This will install the files in your local maven repo at `~/.m2/repository/cash/z/ecc/android/`
```zsh
./gradlew publishReleasePublicationToMavenLocalRepository
```
3. Publish via the following command:
    1. Snapshot: `./gradlew publishReleasePublicationToMavenCentralRepository -PIS_SNAPSHOT=true`
    2. Release
        1. `./gradlew publishReleasePublicationToMavenCentralRepository -PIS_SNAPSHOT=false`
        2. Log into the Sonatype portal to complete the process of closing and releasing the repository.

### Artifacts availability 
- Our existing release artifacts can be found here and here:
   - https://search.maven.org/artifact/cash.z.ecc.android/zcash-android-sdk
   - https://repo1.maven.org/maven2/cash/z/ecc/android/

- And our snapshot artifacts here:
   - https://oss.sonatype.org/content/repositories/snapshots/cash/z/ecc/android/

### Obtain new user token
1. Log in to Sonatype Nexus Repository Manager:
   - Go to https://oss.sonatype.org/ and log in with your Sonatype credentials
1. Access User Token Page:
   - Click on your username in the top right corner
   - Select **Profile** from the dropdown menu
   - In the Profile tab, select **User Token** from the dropdown menu
1. Generate a New Token:
   - Click the **Access User Token** button
   - Re-enter your credentials for authentication
   - A new token will be generated
