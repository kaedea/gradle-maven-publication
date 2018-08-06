/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.Project
import org.junit.Before
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables

import static com.kaedea.gradle.publication.Extension.RELEASE_REPOSITORY_URL
import static com.kaedea.gradle.publication.Extension.SNAPSHOT_REPOSITORY_URL

class ExtensionTest {

    private Project project
    private Extension extension
    private EnvironmentVariables environmentVariables = EnvironmentVariables()

    def opt = { extension.get(it) ?: Utils.readFromProperties(project, it) }
    def mock = { "mock.$it" }
    def env = {
        def value = mock("env.$it")
        environmentVariables.set(it, value)
        return value
    }
    def pro = {
        def value = mock("pro.$it")
        project.setProperty(it, value)
        return value
    }

    @Before
    void setUp() {
        project = ProjectBuilder.builder().withName("project").build()
        extension = new Extension()
    }

    @Test
    void defaultReleaseRepositoryUrl() {
        assertThat(opt(RELEASE_REPOSITORY_URL)).isNull()
        def environment = env(RELEASE_REPOSITORY_URL)
        assertThat(opt(RELEASE_REPOSITORY_URL)).isEqualTo(environment)
        def property = pro(RELEASE_REPOSITORY_URL)
        assertThat(opt(RELEASE_REPOSITORY_URL)).isEqualTo(property)
    }

    @Test
    void defaultSnapshotRepositoryUrl() {
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isNull()
        def environment = env(SNAPSHOT_REPOSITORY_URL)
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isEqualTo(environment)
        def property = pro(SNAPSHOT_REPOSITORY_URL)
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isEqualTo(property)
    }
}