# Build
There are a variety of aspects to building the SDK and demo app. Although much of this ends up being tested automatically by the CI server, understanding these step can help when troubleshooting build issues. These test cases provide sanity checks that the build is not broken.

# Build
1. Run the assemble Gradle task, e.g. `./gradlew assemble`
1. Verify the build completes successfully

# Build tests
1. Run the assembleAndroidTest Gradle task, e.g. `./gradlew assembleAndroidTest`
1. Verify the build completes successfully

# Publishing
1. Run the task `./gradlew publishToMavenLocal`
1. Open the `~/.m2` directory and look for the newly published artifact
1. Verify the coordinate are correct, e.g. `cash/z/ecc/android/zcash-android-sdk`
1. Verify the version is correct, matching the version set in `gradle.properties`
1. Verify the file size looks correct—the AAR should be on the order of 9 megabytes, which indicates that the native libraries have been bundled in

# Clean builds
_To have a clean build, both the local Gradle outputs as well as the Rust outputs need to be deleted.  The Rust outputs are stored in the directory `sdk-lib/targets`.  The `clean` task has been modified to delete the folder `sdk-lib/targets` if it exists._
1. Run the clean Gradle task, e.g. `./gradlew clean`
1. Run the assemble Gradle task, forcing the Gradle build cache to be ignored, e.g. `./gradlew assemble --rerun-tasks`. _Note that `--rerun-tasks` also causes the cache under `~/.gradle/` to be ignored_
1. Verify that the build completes successfully