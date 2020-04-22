# Continuous Integration

In order to ensure code changes comply with the target integrations, the following policies should be observed.

## Changes to the GitHub project

The `master` branch is the git repositories default branch.

Pull Request actions are determined by the author's association to the GitHub project. Levels are defined https://developer.github.com/v4/enum/commentauthorassociation/.

### When a PR is opened by COLLABORATOR

- check that linting is successful
- check that the code builds

### When a PR is opened by NONE

- check that linting is successful

### Code changes to the master branch

These changes are identified by modifications to files matching `^.*\.(kt|proto|rs)$`

- build the project, save artifacts to GCP bucket with short git hash, and `master`
- send notification to Slack channel

### New tag is created

- build the project, save artifacts to GCP bucket with tag
- push project to bintray
- send notification to Slack channel

## Time based changes

Period continous integration tasks

### Nightly

- run integration test

## Upstream changes

Any changes to our dependencies that may necessitate CI.