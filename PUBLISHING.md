# Publishing

Publishing requires:

### One time only
* Get your dev environment setup to [compile the SDK](https://github.com/zcash/zcash-android-wallet-sdk/tree/6d5cf6eefb3c2f793b66162c513790d86f368e49#compiling-sources)
* copy the GPG key to a directory with proper permissions (chmod 600)
* Create file `~/.gradle/gradle.properties` per the [instructions in this guide](https://proandroiddev.com/publishing-a-maven-artifact-3-3-step-by-step-instructions-to-mavencentral-publishing-bd661081645d)
  * add your sonotype credentials to it
  * point it to the GPG key


### Every time
1. Update the [build number](https://github.com/zcash/zcash-android-wallet-sdk/blob/6d5cf6eefb3c2f793b66162c513790d86f368e49/config.gradle) and the [CHANGELOG](https://github.com/zcash/zcash-android-wallet-sdk/blob/6d5cf6eefb3c2f793b66162c513790d86f368e49/CHANGELOG.md)
2. Build locally    
    * Critical Note: Building once does not copy the *.so files and results in an artifact <1MB in size. Building twice fixes that problem and results in an artifact >5MB in size. This is probably a bug in the gradle cargo plugin that we use.
    * This will install the files in your local maven repo at ~/.m2/repository/cash/z/ecc/android/
    * Build twice (first with a clean, then without):
```zsh
./gradlew clean publishToMavenLocal && ./gradlew publishToMavenLocal
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

