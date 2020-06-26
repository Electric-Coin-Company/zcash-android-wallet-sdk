# Continuous Integration

In order to ensure code changes comply with the target integrations, the following policies should be observed.

## Changes to the GitHub project

The `master` branch is the git repositories default branch.

Jobs are managed by PR labels. The following labels are actionable.

|label|targets|
|----|---|
|`safe-to-ci`| `ciBuild`, `ciLintPr`

### Code changes to the master branch

These changes are identified by modifications to files under the `src` directory or files matching `^.*\.(gradle|toml)$`

- build the project
- save artifacts to GCP bucket with short git hash, and `master`
- update code documentation via `docs` gradle target

### New tag is created

- build the project, save artifacts to GCP bucket with tag
- push artifacts to bintray
- send notification to Slack channel

## Time based changes

Periodic continous integration tasks

### Nightly

- run integration tests
- send notifications for failed builds or failed nightly integrations

## Targets

Run gradle targets with the following command.  
Note: it is recommended to NOT install `gradle` because the wrapper command (gradlew) takes care of that automatically.
```
./gradlew <target>
```
Where `<target>` is one of the following

- **ciBuild** : build the project and produce the AAR artifact under `build/outputs/aar`
- **ciDeploy** : deploy the AAR artifact to bintray. This will invoke **ciBuild**, if necessary.
- **ciLintPr** : lint the code in response to a PR to verify codestyle and formatting.
- **ciTestPr** : run the basic PR test suite. This will invoke **ciBuild**, if necessary.
- **ciTestNightly** : run the full nightly integration test suite
