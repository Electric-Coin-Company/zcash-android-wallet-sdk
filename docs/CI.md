# Continuous Integration
Continuous integration is set up with GitHub Actions.  The workflows are defined in this repo under [/.github/workflows](../.github/workflows).

Workflows exist for:
 * Pull request - On pull request, static analysis and testing is performed.
 * Snapshot deployment - On merge to the main branch, a snapshot release is deployed to Maven Central.  Concurrency limits are in place, to ensure that only one snapshot deployment can happen at a time.
 * Release deployment - Manually invoked workflow to deploy to Maven Central.  Concurrency limits are in place, to ensure that only one release deployment can happen at a time.
 
## Setup
When forking this repository, some vars/secrets need to be defined to set up new continuous integration builds.

The vars/secrets passed to GitHub Actions then map to Gradle properties set up within our build scripts.  Necessary secrets are documented at the top of each GitHub workflow yml file, as well as reiterated here.

### Pull request
* Variables
    * `FIREBASE_TEST_LAB_PROJECT` - Firebase Test Lab project name.
* Secrets
    * `EMULATOR_WTF_API_KEY` - API key for [Emulator.wtf](https://emulator.wtf)
    * `FIREBASE_TEST_LAB_SERVICE_ACCOUNT` - Email address of Firebase Test Lab service account.
    * `FIREBASE_TEST_LAB_WORKLOAD_IDENTITY_PROVIDER` - Workload identity provider to generate temporary service account key.

The Pull Request workflow supports testing of the app and libraries with both Emulator.wtf and Firebase Test Lab.  By default Emulator.wtf is used for library instrumentation tests, while Firebase Test Lab is used for a robo test.

To configure Firebase Test Lab, you'll need to enable the necessary Google Cloud APIs to enable automated access to Firebase Test Lab.
* Configure Firebase Test Lab.  Google has [documentation for Jenkins](https://firebase.google.com/docs/test-lab/android/continuous).  Although we're using GitHub Actions, the initial requirements are the same.
* Configure [workload identity federation](https://github.com/google-github-actions/auth#setting-up-workload-identity-federation)

Note: Pull requests do not currently run darkside tests.  See #361.

### Snapshot deployment
* Secrets
    * `MAVEN_CENTRAL_USERNAME` — Username for Maven Central, which maps to the Gradle property `mavenCentralUsername`.
    * `MAVEN_CENTRAL_PASSWORD` — Password for Maven Central, which maps to the Gradle property `mavenCentralPassword`.

GPG keys are not needed for snapshot deployment.

Note: For documentation on the Gradle properties for Maven deployment, see [Gradle Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin).

Note: Snapshot builds are configured with a Gradle property `IS_SNAPSHOT`.  The workflow automatically sets this property to true for snapshot deployments.  This will suffix the version with `-SNAPSHOT` and will upload to the snapshot repository.

### Release deployment
* Secrets
    * `MAVEN_CENTRAL_USERNAME` — Username for Maven Central, which maps to the Gradle property `mavenCentralUsername`.
    * `MAVEN_CENTRAL_PASSWORD` — Password for Maven Central, which maps to the Gradle property `mavenCentralPassword`.
    * `MAVEN_SIGNING_KEYRING_FILE_BASE64` — GPG keyring file, base64 encoded.  Maps to Gradle property `signing.secretKeyRingFile`.
    * `MAVEN_SIGNING_KEY_ID` — Name of key inside GPG keyring file.  Maps to Gradle property `signing.keyId`.
    * `MAVEN_SIGNING_PASSWORD` — Password for key inside GPG keyring file.  Maps to Gradle property `signing.password`.

Note: For documentation on the Gradle properties for Maven deployment, see [Gradle Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin).

Note: Snapshot builds are configured with a Gradle property `IS_SNAPSHOT`.  The workflow automatically sets this property to false for release deployments.
