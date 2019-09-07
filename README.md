# semver-release-maven-plugin

Semantic versioning release maven plugin

[![Build](https://img.shields.io/gitlab/pipeline/lorislab/maven/semver-release-maven-plugin?style=for-the-badge&logo=gitlab)](https://gitlab.com/lorislab/maven/semver-release-maven-plugin/pipelines)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.lorislab.maven/semver-release-maven-plugin?style=for-the-badge&maven)](https://maven-badges.herokuapp.com/maven-central/org.lorislab.maven/semver-release-maven-plugin)


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
* skipPush - skip push git changes to remote repository. Defaut: false

## Patch

Create a patch branch and set patch version to 1.

```bash
mvn semver-release:patch-create
```

Properties:
* skipPush - skip push git changes to remote repository. Defaut: false
* patchVersion - create patch from this version (a.b.0). Default: null,inteactive mode.