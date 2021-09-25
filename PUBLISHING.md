# Publishing

Publishing requires:

### One time only
* Get your dev environment setup to [compile the SDK](https://github.com/zcash/zcash-android-wallet-sdk/#compiling-sources)
* copy the GPG key to a directory with proper permissions (chmod 600). Note: If you'd like to quickly locally without subsequently publishing to Maven Central, configure a Gradle property `RELEASE_SIGNING_ENABLED=false`
* Create file `~/.gradle/gradle.properties` per the [instructions in this guide](https://proandroiddev.com/publishing-a-maven-artifact-3-3-step-by-step-instructions-to-mavencentral-publishing-bd661081645d)
  * add your sonotype credentials to it
  * point it to the GPG key


### Every time
1. Update the [build number](https://github.com/zcash/zcash-android-wallet-sdk/blob/master/gradle.properties) and the [CHANGELOG](https://github.com/zcash/zcash-android-wallet-sdk/blob/master/CHANGELOG.md)
2. Build locally
    * This will install the files in your local maven repo at `~/.m2/repository/cash/z/ecc/android/`
```zsh
./gradlew publishToMavenLocal
```
3. Publish via the following command:
```zsh
# This uploads the file to sonotype’s staging area
./gradlew publish --no-daemon --no-parallel
```
4. Deploy to maven central:
```zsh
# This closes the staging repository and releases it to the world
./gradlew closeAndReleaseRepository
```

Note:
Our existing artifacts can be found here and here:
https://search.maven.org/artifact/cash.z.ecc.android/zcash-android-sdk
https://repo1.maven.org/maven2/cash/z/ecc/android/
