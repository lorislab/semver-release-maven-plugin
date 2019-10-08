# semver-release-maven-plugin

Semantic versioning release maven plugin

[![License](https://img.shields.io/github/license/lorislab/semver-release-maven-plugin?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![CircleCI](https://img.shields.io/circleci/build/github/lorislab/semver-release-maven-plugin?logo=circleci&style=for-the-badge)](https://circleci.com/gh/lorislab/semver-release-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/org.lorislab.maven/semver-release-maven-plugin?logo=java&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/org.lorislab.maven/semver-release-maven-plugin)
[![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/lorislab/semver-release-maven-plugin?logo=github&style=for-the-badge)](https://github.com/lorislab/semver-release-maven-plugin/releases/latest)

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

# Release

## Release process of this plugin

Create new release run
```bash
mvn semver-release:release-create
```

Create new patch branch run
```bash
mvn semver-release:patch-create -DpatchVersion=X.X.0
```