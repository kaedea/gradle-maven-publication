/*
 * Copyright (C) 2018 Vanniktech - Niklas Baudy
 * Licensed under the Apache License, Version 2.0
 */

package com.kaedea.gradle.publication

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.plugins.GroovyPlugin
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import static org.assertj.core.api.Assertions.assertThat

/**
 * Copycat of {@link "https://github.com/vanniktech/gradle-maven-publish-plugin/blob/master/src/test/kotlin/com/vanniktech/maven/publish/MavenPublishPluginTest.kt"}.
 */
@RunWith(JUnit4.class)
class PluginTest {

    @Test
    void javaPlugin() {
        def project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaPlugin.class)
        assure(project)
    }

    @Test
    void javaLibraryPlugin() {
        def project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaLibraryPlugin.class)
        assure(project)
    }

    @Test
    @Ignore
    void javaLibraryPluginWithGroovy() {
        def project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaLibraryPlugin.class)
        project.plugins.apply(GroovyPlugin.class)
        assure(project)

        assertThat(project.tasks.getByName("groovydocJar")).isNotNull()
    }

    @Test
    void androidLibraryPlugin() {
        def project = ProjectBuilder.builder().build()
        project.plugins.apply(LibraryPlugin.class)

        prepareAndroidLibraryProject(project)
        assure(project)
    }

    @Test
    @Ignore
    void androidLibraryPluginWithKotlinAndroid() {
        def project = ProjectBuilder.builder().build()
        project.plugins.apply(LibraryPlugin.class)
        project.plugins.apply("kotlin-android")

        prepareAndroidLibraryProject(project)
        assure(project)
    }

    @Test
    @Ignore
    void javaLibraryPluginWithKotlin() {
        def project = ProjectBuilder.builder().withName("single").build() as DefaultProject
        project.plugins.apply(JavaLibraryPlugin.class)
        project.plugins.apply("kotlin")
        assure(project)
    }

    private void prepareAndroidLibraryProject(Project project) {
        def extension = project.extensions.getByType(LibraryExtension.class)
        extension.compileSdkVersion(27)

        File manifestFile = new File(project.projectDir, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile <<"<manifest package=\"com.foo.bar\"/>"
    }


    private void assure(Project project) {
        project.plugins.apply(PublicationPlugin.class)

        def extension = project.extensions.getByType(Extension.class)
        extension.GROUP("bar")
        extension.VERSION_NAME("foo")

        (project as DefaultProject).evaluate()

        assertThat(project.plugins.findPlugin(MavenPlugin.class)).isNotNull()
        assertThat(project.plugins.findPlugin(SigningPlugin.class)).isNotNull()
        assertThat(project.group).isNotNull()
        assertThat(project.version).isNotNull()

        def task = project.tasks.getByName("uploadArchives")
        assertThat(task).isNotNull()
        assertThat(task.group).isEqualTo("upload")
    }
}