# Publishing

Two types of artifacts can be published:
1. Snapshot — An unstable release of the SDK for testing
1. Release — A stable release of the SDK

Control of these modes of release is managed with a Gradle property `IS_SNAPSHOT`, which is 

For both snapshot and release deployments, there are two ways to initiate deployment:
1. Automatically — See [ci.md](ci.md), which describes the continuous integration workflow for deployment.
2. Manually — See the remainder of this document for how to configure manual deployment.

Publishing requires:

### One time only
* Set up environment to [compile the SDK](https://github.com/zcash/zcash-android-wallet-sdk/#compiling-sources)
* Copy the GPG key to a directory with proper permissions (chmod 600). Note: If you'd like to quickly publish locally without subsequently publishing to Maven Central, configure a Gradle property `RELEASE_SIGNING_ENABLED=false`
* Create file `~/.gradle/gradle.properties` per the [instructions in this guide](https://proandroiddev.com/publishing-a-maven-artifact-3-3-step-by-step-instructions-to-mavencentral-publishing-bd661081645d)
  * add your sonotype credentials with these properties
      * `mavenCentralUsername`
      * `mavenCentralPassword`
  * point it to the GPG key with these properties
     * `signing.keyId`
     * `signing.password`
     * `signing.secretKeyRingFile`

### Every time
1. Update the [build number](https://github.com/zcash/zcash-android-wallet-sdk/blob/master/gradle.properties) and the [CHANGELOG](https://github.com/zcash/zcash-android-wallet-sdk/blob/master/CHANGELOG.md).  For release builds, suffix the Gradle invocations below with `-PIS_SNAPSHOT=false`.
3. Build locally
    * This will install the files in your local maven repo at `~/.m2/repository/cash/z/ecc/android/`
```zsh
./gradlew publishToMavenLocal
```
4. Publish via the following command:
```zsh
# This uploads the file to sonotype’s staging area
./gradlew publish --no-daemon --no-parallel
```
5. Deploy to maven central:
```zsh
# This closes the staging repository and releases it to the world
./gradlew closeAndReleaseRepository
```

Note:
Our existing artifacts can be found here and here:
https://search.maven.org/artifact/cash.z.ecc.android/zcash-android-sdk
https://repo1.maven.org/maven2/cash/z/ecc/android/
