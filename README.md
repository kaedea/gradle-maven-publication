# Publications

Gradle scripts/plugin that helps to publish jar/aar artifacts with gradle `maven` or `maven-publish` plugin.


## Project Structure

|  Demo projects |  Description |
| :------------ | :------------ |
| app | Demo app module |
| lib-android | Demo android library module |
| lib-android-sub | Demo android library module 2 |
| lib-java | Demo java library module |
| lib-java-sub | Demo java library module  2 |

|  Script |  Description |
| :------------ | :------------ |
| gradle/maven.gradle | Workaround script with gradle plugin 'maven' |
| gradle/maven-dcendents.gradle | Workaround script with 3rd-party gradle plugin 'android-maven' |
| gradle/maven-publish.gradle | Workaround script with gradle plugin 'maven-publish' |

|  Plugin |  Description |
| :------------ | :------------ |
| gradle-plugin/publication | Custom gradle plugin that helps to publish jar/aar artifacts (WIP) |

## Getting Started

### Script

Config the project properties like:
```shell
# ----------
# Maven repository config
# ----------
RELEASE_REPOSITORY_URL=(here or system env)
SNAPSHOT_REPOSITORY_URL=(here or system env)

NEXUS_USERNAME=(here or system env)
NEXUS_PASSWORD=(here or system env)

POM_URL=https://github.com/kaedea/publication/
POM_SCM_URL=https://github.com/kaedea/publication/
POM_SCM_CONNECTION=scm:git:git://github.com/kaedea/publication.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://git@github.com:kaedea/publication.git

POM_LICENCE_NAME=The Apache Software License, Version 2.0
POM_LICENCE_URL=http://www.apache.org/licenses/LICENSE-2.0.txt
POM_LICENCE_DIST=repo

POM_DEVELOPER_ID=kaedea
POM_DEVELOPER_NAME=Kaede Akatsuki

# ----------
# Maven artifact config
# ----------
GROUP=com.kaedea
VERSION_NAME=0.1.0-SNAPSHOT

POM_NAME=Publication Android Library
POM_ARTIFACT_ID=publication-android-library
POM_PACKAGING=aar
POM_DESCRIPTION=Demo android library of project Publications
```

Apply the script in your project's build.gradle:
```groovy
// For leagcy 'maven' plugin:
apply from: 'http://kaedea.github.com/publication/gradle/maven.gradle'

// For dcendents's 'android-maven' plugin:
apply from: 'http://kaedea.github.com/publication/gradle/maven-dcendents.gradle'

// For new 'maven-publish' plugin:
apply from: 'http://kaedea.github.com/publication/gradle/maven-publish.gradle'
```

At last, run the following tasks to publish:
```bash
# For leagcy 'maven' or dcendents's 'android-maven' plugin:
gradle :uploadArchives

# For new 'maven-publish' plugin:
gradle :generatePomFileForArchivesPublication
gradle :publishToMavenLocal
gradle :publish
```

### Upload to Bintray
Config the project properties like:
```shell
# ----------
# Bintray auth config
# ----------
BINTRAY_USERNAME=
BINTRAY_API_KEY=

# ----------
# Maven repo config
# ----------
BINTRAY_REPO=maven
BINTRAY_NAME=publication-android
```

Apply the script in your project's build.gradle as [above](#Script), then apply the bintray script:
```groovy
apply from: 'http://kaedea.github.com/publication/gradle/bintray.gradle'

```

Run the following tasks to publish to Bintray:
```bash
gradle :bintrayUpload
```

### Plugin

Work in progress

## License

The project is [Apache License, Version 2.0](/LICENSE) licensed.