/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import static com.kaedea.gradle.publication.Extension.RELEASE_REPOSITORY_URL
import static com.kaedea.gradle.publication.Extension.SNAPSHOT_REPOSITORY_URL
import static org.assertj.core.api.Assertions.assertThat

@RunWith(JUnit4.class)
class ExtensionTest {

    private Project project
    private Extension extension

    @Rule
    public TestRule environmentVariables = new EnvironmentVariables()

    def opt = { extension.get(it) ?: Utils.readFromProperties(project, it)?.toString() }
    def mock = { "mock.$it" }
    def env = {
        def value = mock("env.$it")
        environmentVariables.set(it, value)
        return value
    }
    def pro = {
        def value = mock("pro.$it")
        project.extensions.add(it, value)
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
        String environment = env(RELEASE_REPOSITORY_URL)
        assertThat(environment).isNotBlank()
        assertThat(opt(RELEASE_REPOSITORY_URL)).isEqualTo(environment)
        String property = pro(RELEASE_REPOSITORY_URL)
        assertThat(property).isNotBlank()
        assertThat(opt(RELEASE_REPOSITORY_URL)).isEqualTo(property)
    }

    @Test
    void defaultSnapshotRepositoryUrl() {
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isNull()
        String environment = env(SNAPSHOT_REPOSITORY_URL)
        assertThat(environment).isNotBlank()
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isEqualTo(environment)
        String property = pro(SNAPSHOT_REPOSITORY_URL)
        assertThat(property).isNotBlank()
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isEqualTo(property)
    }
}