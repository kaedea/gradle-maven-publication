/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication
/**
 * Extension of {@link PublicationPlugin}
 */
class Extension {

    public static final String GROUP = 'GROUP'
    public static final String VERSION_NAME = 'VERSION_NAME'

    public static final String POM_NAME = 'POM_NAME'
    public static final String POM_ARTIFACT_ID = 'POM_ARTIFACT_ID'
    public static final String POM_PACKAGING = 'POM_PACKAGING'
    public static final String POM_URL = 'POM_URL'
    public static final String POM_DESCRIPTION = 'POM_DESCRIPTION'

    public static final String POM_SCM_URL = 'POM_SCM_URL'
    public static final String POM_SCM_CONNECTION = 'POM_SCM_CONNECTION'
    public static final String POM_SCM_DEV_CONNECTION = 'POM_SCM_DEV_CONNECTION'

    public static final String POM_LICENCE_NAME = 'POM_LICENCE_NAME'
    public static final String POM_LICENCE_URL = 'POM_LICENCE_URL'
    public static final String POM_LICENCE_DIST = 'POM_LICENCE_DIST'

    public static final String POM_DEVELOPER_ID = 'POM_DEVELOPER_ID'
    public static final String POM_DEVELOPER_NAME = 'POM_DEVELOPER_NAME'

    public static final String RELEASE_REPOSITORY_URL = 'RELEASE_REPOSITORY_URL'
    public static final String SNAPSHOT_REPOSITORY_URL = 'SNAPSHOT_REPOSITORY_URL'
    public static final String NEXUS_USERNAME = 'NEXUS_USERNAME'
    public static final String NEXUS_PASSWORD = 'NEXUS_PASSWORD'

    public static final String BINTRAY_REPO = 'BINTRAY_REPO'
    public static final String BINTRAY_NAME = 'BINTRAY_NAME'
    public static final String BINTRAY_USERNAME = 'BINTRAY_USERNAME'
    public static final String BINTRAY_API_KEY = 'BINTRAY_API_KEY'

    Boolean jarSources = Boolean.TRUE
    Boolean jarJavaDoc = Boolean.FALSE
    Boolean jarTests = Boolean.FALSE
    Boolean signing = Boolean.FALSE
    Boolean uploadToBintray = Boolean.FALSE
    Map<String, String> properties = new HashMap<>()

    String get(String key) {
        properties.get(key)
    }

    void GROUP(String value) {
        properties.put(GROUP, value)
    }

    void VERSION_NAME(String value) {
        properties.put(VERSION_NAME, value)
    }

    void POM_NAME(String value) {
        properties.put(POM_NAME, value)
    }

    void POM_ARTIFACT_ID(String value) {
        properties.put(POM_ARTIFACT_ID, value)
    }

    void POM_PACKAGING(String value) {
        properties.put(POM_PACKAGING, value)
    }

    void POM_URL(String value) {
        properties.put(POM_URL, value)
    }

    void POM_DESCRIPTION(String value) {
        properties.put(POM_DESCRIPTION, value)
    }

    void POM_SCM_URL(String value) {
        properties.put(POM_SCM_URL, value)
    }

    void POM_SCM_CONNECTION(String value) {
        properties.put(POM_SCM_CONNECTION, value)
    }

    void POM_SCM_DEV_CONNECTION(String value) {
        properties.put(POM_SCM_DEV_CONNECTION, value)
    }

    void POM_LICENCE_NAME(String value) {
        properties.put(POM_LICENCE_NAME, value)
    }

    void POM_LICENCE_URL(String value) {
        properties.put(POM_LICENCE_URL, value)
    }

    void POM_LICENCE_DIST(String value) {
        properties.put(POM_LICENCE_DIST, value)
    }

    void POM_DEVELOPER_ID(String value) {
        properties.put(POM_DEVELOPER_ID, value)
    }

    void POM_DEVELOPER_NAME(String value) {
        properties.put(POM_DEVELOPER_NAME, value)
    }

    void RELEASE_REPOSITORY_URL(String value) {
        properties.put(RELEASE_REPOSITORY_URL, value)
    }

    void SNAPSHOT_REPOSITORY_URL(String value) {
        properties.put(SNAPSHOT_REPOSITORY_URL, value)
    }

    void NEXUS_USERNAME(String value) {
        properties.put(NEXUS_USERNAME, value)
    }

    void NEXUS_PASSWORD(String value) {
        properties.put(NEXUS_PASSWORD, value)
    }

    void BINTRAY_REPO(String value) {
        properties.put(BINTRAY_REPO, value)
    }

    void BINTRAY_NAME(String value) {
        properties.put(BINTRAY_NAME, value)
    }

    void BINTRAY_USERNAME(String value) {
        properties.put(BINTRAY_USERNAME, value)
    }

    void BINTRAY_API_KEY(String value) {
        properties.put(BINTRAY_API_KEY, value)
    }
}
