# semver-release-maven-plugin

Semantic versioning release maven plugin

[![License](https://img.shields.io/github/license/lorislab/semver-release-maven-plugin?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/lorislab/semver-release-maven-plugin/build/master?logo=github&style=for-the-badge)](https://github.com/lorislab/semver-release-maven-plugin/actions?query=workflow%3Abuild)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/lorislab/semver-release-maven-plugin?sort=semver&logo=github&style=for-the-badge)](https://github.com/lorislab/semver-release-maven-plugin/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/org.lorislab.maven/semver-release-maven-plugin?logo=java&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/org.lorislab.maven/semver-release-maven-plugin)


# Properties

## Git version 

Set maven project version to git version a.b.c-GIT_HASH:7
Replace the snapshot suffix with the git hash (7).

```bash
mvn semver-release:version-git-hash
```

Properties:
* abbrevLength - git hash length. Default: 7

## Release version

Set maven project version to release version: a.b.c
This command remove the snapshot suffix

```bash
mvn semver-release:version-release
```

## Release

Create git tag and increment the development version.

```bash
mvn semver-release:release-create
```

Properties:
* skipPush - skip push git changes to remote repository. Default: false

## Patch

Create a patch branch and set patch version to 1.

```bash
mvn semver-release:patch-create
```

Properties:
* skipPush - skip push git changes to remote repository. Default: false
* patchVersion - create patch from this version (a.b.0). Default: null,interactive mode.
