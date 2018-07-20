/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningPlugin

/**
 * Custom gradle plugin that helps to apply the gradle publishing plugin {@link MavenPlugin}.
 * @see "https://docs.gradle.org/current/userguide/maven_plugin.html"
 */
class PublicationPlugin implements Plugin<Project> {

    def project
    def uploadTaskPath = {
        project.rootProject == project ?
                ":uploadArchives" :
                "$project.path:uploadArchives"
    }

    def installTaskPath = {
        project.rootProject == project ?
                ":$MavenPlugin.INSTALL_TASK_NAME" :
                "$project.path:$MavenPlugin.INSTALL_TASK_NAME"
    }

    def required = { Utils.readFromPropertiesVital(project, it) }
    def optionally = { Utils.readFromProperties(project, it) }

    def repositoryUsername = { Utils.readFromProperties(project, 'NEXUS_USERNAME', true) }
    def repositoryPassword = { Utils.readFromProperties(project, 'NEXUS_PASSWORD', true) }
    def releaseRepositoryUrl = { Utils.readFromProperties(project, 'RELEASE_REPOSITORY_URL') }
    def snapshotRepositoryUrl = { Utils.readFromProperties(project, 'SNAPSHOT_REPOSITORY_URL') }

    @Override
    void apply(Project project) {
        this.project = project

        project.logger.lifecycle "----------"
        project.logger.lifecycle "Publication: apply gradle maven publishing tasks..."
        project.logger.lifecycle "----------"

        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)
        project.group = required('GROUP')
        project.version = required('VERSION_NAME')

        configureArtifactTasks()
        configurePom()
        configureUpload()
        configureSigning()
    }

    private void configureArtifactTasks() {
        project.afterEvaluate {
            project.plugins.withType(JavaPlugin) {
                configureSourcesJarTask()
                configureJavadocJarTask()
                configureTestsJarTask()
            }

            project.tasks.withType(JavaCompile) {
                options.encoding = "UTF-8"
            }

            project.tasks.withType(Javadoc).all {
                options.encoding = "UTF-8"
                options.addStringOption('encoding', 'UTF-8')
                if (JavaVersion.current().isJava8Compatible()) {
                    options.addStringOption('Xdoclint:none', '-quiet')
                }
            }

            addArtifactTask("sourcesJar")
            addArtifactTask("javadocJar")
            addArtifactTask("testsJar")
        }
    }

    private void configureSourcesJarTask() {
        project.task('sourcesJar', type: Jar) {
            classifier = 'sources'
            group = 'build'
            description = 'Assembles a jar archive containing the main sources of this mProject.'
            from project.sourceSets.main.allSource
        }
    }

    private void configureJavadocJarTask() {
        project.task('javadocJar', type: Jar) {
            classifier = 'javadoc'
            group = 'build'
            description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
            from project.plugins.hasPlugin(GroovyPlugin) ?
                    project.tasks.getByName(GroovyPlugin.GROOVYDOC_TASK_NAME) :
                    project.tasks.getByName(JavaPlugin.JAVADOC_TASK_NAME)
        }
    }

    private void configureTestsJarTask() {
        project.task('testsJar', type: Jar) {
            classifier = 'tests'
            group = 'build'
            description = 'Assembles a jar archive containing the test sources of this project.'
            from project.sourceSets.test.output
        }
    }

    private void addArtifactTask(String taskName) {
        Task task = project.tasks.findByName(taskName)
        if (task) {
            project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task)
        }
    }

    private void configurePom() {
        project.afterEvaluate {
            project.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                pom.project {
                    groupId required('GROUP')
                    artifactId required('POM_ARTIFACT_ID')
                    version required('VERSION_NAME')

                    name optionally('POM_NAME') ?: project.name
                    packaging optionally('POM_PACKAGING') ?: 'jar'
                    url optionally('POM_URL')
                    description optionally('POM_DESCRIPTION')

                    scm {
                        url optionally('POM_SCM_URL')
                        connection optionally('POM_SCM_CONNECTION')
                        developerConnection optionally('POM_SCM_DEV_CONNECTION')
                    }
                    licenses {
                        license {
                            name optionally('POM_LICENCE_NAME')
                            url optionally('POM_LICENCE_URL')
                            distribution optionally('POM_LICENCE_DIST')
                        }
                    }
                    developers {
                        developer {
                            id optionally('POM_DEVELOPER_ID')
                            name optionally('POM_DEVELOPER_NAME')
                        }
                    }
                }

                def scopeMappings = pom.scopeMappings
                def addDependency = { configuration, scope ->
                    if (configuration != null) scopeMappings.addMapping(1, configuration, scope)
                }
                addDependency(project.configurations.implementation, 'compile')
                addDependency(project.configurations.compileOnly, 'provided')
                addDependency(project.configurations.runtimeOnly, 'runtime')
            }
        }
    }

    private void configureUpload() {
        project.afterEvaluate {
            project.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if (taskGraph.hasTask(uploadTaskPath())) {

                        if (!releaseRepositoryUrl() && !snapshotRepositoryUrl()) {
                            // publish to local maven
                            repository(url: project.uri(project.rootProject.file('maven')))
                        }

                        if (releaseRepositoryUrl()) {
                            repository(url: releaseRepositoryUrl()) {
                                authentication(
                                        userName: repositoryUsername(),
                                        password: repositoryPassword()
                                )
                            }
                        }
                        if (snapshotRepositoryUrl()) {
                            snapshotRepository(url: snapshotRepositoryUrl()) {
                                authentication(
                                        userName: repositoryUsername(),
                                        password: repositoryPassword()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private void configureSigning() {
        project.afterEvaluate {
            project.gradle.taskGraph.whenReady {
                project.tasks
                        .withType(Upload)
                        .matching { it.path == uploadTaskPath() }
                        .each {
                    it.repositories.mavenDeployer() {
                        beforeDeployment {
                            MavenDeployment deployment -> project.signing.signPom(deployment)
                        }
                    }
                }
                project.tasks
                        .withType(Upload)
                        .matching { it.path == installTaskPath() }
                        .each {
                    it.repositories.mavenDeployer() {
                        beforeDeployment {
                            MavenDeployment deployment -> project.signing.signPom(deployment)
                        }
                    }
                }
            }
        }
        project.signing {
            required {
                Utils.isReleaseBuild(project) && project.gradle.taskGraph.hasTask("uploadArchives")
            }
            sign project.configurations.archives
        }
    }
}
