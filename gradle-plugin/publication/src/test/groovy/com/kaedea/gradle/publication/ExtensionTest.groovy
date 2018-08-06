/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import static com.kaedea.gradle.publication.Extension.*
import static org.assertj.core.api.Assertions.assertThat

@RunWith(JUnit4.class)
class ExtensionTest {

    private def project
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

        extension.with {
            jarSources = true
            jarJavaDoc = true
            jarTests = true
            uploadToBintray = true
        }
        extension.GROUP('com.kaedea')
        extension.VERSION_NAME('0.1.0-test')
    }

    @Test
    void testConfigureFromPluginExtension() {
        assertThat(extension.jarSources).isEqualTo(true)
        assertThat(extension.jarJavaDoc).isEqualTo(true)
        assertThat(extension.jarTests).isEqualTo(true)
        assertThat(extension.uploadToBintray).isEqualTo(true)
        assertThat(opt(GROUP)).isEqualTo('com.kaedea')
        assertThat(opt(VERSION_NAME)).isEqualTo('0.1.0-test')
    }

    @Test
    void testConfigureFromPluginEnvironment() {
        assertThat(opt(RELEASE_REPOSITORY_URL)).isNull()
        String environment = env(RELEASE_REPOSITORY_URL)
        assertThat(opt(RELEASE_REPOSITORY_URL)).isNotBlank()
        assertThat(opt(RELEASE_REPOSITORY_URL)).isEqualTo(environment)
    }

    @Test
    void testConfigureFromPluginProjectProperty() {
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isNull()
        String property = pro(SNAPSHOT_REPOSITORY_URL)
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isNotBlank()
        assertThat(opt(SNAPSHOT_REPOSITORY_URL)).isEqualTo(property)
    }
}