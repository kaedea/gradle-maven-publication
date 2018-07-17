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

    private Project mProject

    def isReleaseBuild() {
        return mProject.VERSION_NAME.contains("SNAPSHOT") == false
    }

    @Override
    void apply(Project project) {
        project.logger.lifecycle "----------"
        project.logger.lifecycle "Publication: apply gradle maven publishing tasks..."
        project.logger.lifecycle "----------"

        mProject = project
        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)
        project.group = project.GROUP
        project.version = project.VERSION_NAME

        configureArtifactTasks()
        configurePom()
        configureUpload()
        configureSigning()
    }

    private void configureArtifactTasks() {
        mProject.afterEvaluate {
            mProject.plugins.withType(JavaPlugin) {
                configureSourcesJarTask()
                configureJavadocJarTask()
            }

            mProject.tasks.withType(JavaCompile) {
                options.encoding = "UTF-8"
            }

            mProject.tasks.withType(Javadoc).all {
                options.encoding = "UTF-8"
                options.addStringOption('encoding', 'UTF-8')
                if (JavaVersion.current().isJava8Compatible()) {
                    options.addStringOption('Xdoclint:none', '-quiet')
                }
            }

            addArtifactTask("sourcesJar")
            addArtifactTask("javadocJar")
        }
    }

    private void configureSourcesJarTask() {
        mProject.task('sourcesJar', type: Jar) {
            classifier = 'sources'
            group = "build"
            description = 'Assembles a jar archive containing the main sources of this mProject.'
            from mProject.sourceSets.main.allSource
        }
    }

    private void configureJavadocJarTask() {
        mProject.task('javadocJar', type: Jar) {
            classifier = 'javadoc'
            group = "build"
            description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
            from getDocTask()
        }
    }

    private def getDocTask() {
        hasGroovyPlugin() ?
                mProject.tasks.getByName(GroovyPlugin.GROOVYDOC_TASK_NAME) :
                mProject.tasks.getByName(JavaPlugin.JAVADOC_TASK_NAME)
    }

    private boolean hasGroovyPlugin() {
        hasPlugin(GroovyPlugin)
    }

    private boolean hasPlugin(Class<? extends Plugin> pluginClass) {
        mProject.plugins.hasPlugin(pluginClass)
    }

    private void addArtifactTask(String taskName) {
        Task task = mProject.tasks.findByName(taskName)
        if (task) {
            mProject.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task)
        }
    }

    private void configurePom() {
        mProject.afterEvaluate {
            mProject.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                pom.project {
                    groupId mProject.GROUP
                    artifactId mProject.POM_ARTIFACT_ID
                    version mProject.VERSION_NAME

                    name mProject.POM_NAME
                    packaging mProject.POM_PACKAGING
                    url mProject.POM_URL
                    description mProject.POM_DESCRIPTION

                    scm {
                        url mProject.POM_SCM_URL
                        connection mProject.POM_SCM_CONNECTION
                        developerConnection mProject.POM_SCM_DEV_CONNECTION
                    }
                    licenses {
                        license {
                            name mProject.POM_LICENCE_NAME
                            url mProject.POM_LICENCE_URL
                            distribution mProject.POM_LICENCE_DIST
                        }
                    }
                    developers {
                        developer {
                            id mProject.POM_DEVELOPER_ID
                            name mProject.POM_DEVELOPER_NAME
                        }
                    }
                }

                def scopeMappings = pom.scopeMappings
                def addDependency = { configuration, scope ->
                    if (configuration != null) scopeMappings.addMapping(1, configuration, scope)
                }
                addDependency(mProject.configurations.implementation, 'compile')
                addDependency(mProject.configurations.compileOnly, 'provided')
                addDependency(mProject.configurations.runtimeOnly, 'runtime')
            }
        }
    }

    private void configureUpload() {
        mProject.afterEvaluate {
            mProject.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                mProject.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if (taskGraph.hasTask(getUploadTaskPath())) {

                        if (!getReleaseRepositoryUrl() && !getSnapshotRepositoryUrl()) {
                            // publish to local maven
                            repository(url: mProject.uri(mProject.rootProject.file('maven')))
                        }

                        if (getReleaseRepositoryUrl()) {
                            repository(url: getReleaseRepositoryUrl()) {
                                authentication(
                                        userName: getRepositoryUsername(),
                                        password: getRepositoryPassword()
                                )
                            }
                        }
                        if (getSnapshotRepositoryUrl()) {
                            snapshotRepository(url: getSnapshotRepositoryUrl()) {
                                authentication(
                                        userName: getRepositoryUsername(),
                                        password: getRepositoryPassword()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private void configureSigning() {
        mProject.afterEvaluate {
            mProject.gradle.taskGraph.whenReady {
                mProject.tasks
                        .withType(Upload)
                        .matching { it.path == getUploadTaskPath() }
                        .each {
                    it.repositories.mavenDeployer() {
                        beforeDeployment {
                            MavenDeployment deployment -> mProject.signing.signPom(deployment)
                        }
                    }
                }
                mProject.tasks
                        .withType(Upload)
                        .matching { it.path == getInstallTaskPath() }
                        .each {
                    it.repositories.mavenDeployer() {
                        beforeDeployment {
                            MavenDeployment deployment -> mProject.signing.signPom(deployment)
                        }
                    }
                }
            }
        }
        mProject.signing {
            required { isReleaseBuild() && mProject.gradle.taskGraph.hasTask("uploadArchives") }
            sign mProject.configurations.archives
        }
    }

    def getUploadTaskPath() {
        mProject.rootProject == mProject ? ":uploadArchives" : "$mProject.path:uploadArchives"
    }

    def getInstallTaskPath() {
        mProject.rootProject == mProject ? ":$MavenPlugin.INSTALL_TASK_NAME" : "$mProject.path:$MavenPlugin.INSTALL_TASK_NAME"
    }

    def getReleaseRepositoryUrl() {
        return mProject.hasProperty('RELEASE_REPOSITORY_URL') ?
                mProject.RELEASE_REPOSITORY_URL :
                System.env.RELEASE_REPOSITORY_URL
    }

    def getSnapshotRepositoryUrl() {
        return mProject.hasProperty('SNAPSHOT_REPOSITORY_URL') ?
                mProject.SNAPSHOT_REPOSITORY_URL :
                System.env.RELEASE_REPOSITORY_URL
    }

    def getRepositoryUsername() {
        def var = mProject.hasProperty('NEXUS_USERNAME') ?
                mProject.NEXUS_USERNAME :
                System.env.NEXUS_USERNAME
        if (!var) {
            ConsoleHandler consoleHandler = new ConsoleHandler()
            var = consoleHandler.askForUsername()
        }
        return var
    }

    def getRepositoryPassword() {
        def var = mProject.hasProperty('NEXUS_PASSWORD') ?
                mProject.NEXUS_PASSWORD :
                System.env.NEXUS_PASSWORD
        if (!var) {
            ConsoleHandler consoleHandler = new ConsoleHandler()
            var = consoleHandler.askForPassword()
        }
        return var
    }

    private class ConsoleHandler {
        Console console

        ConsoleHandler() {
            console = System.console()
        }

        String askForUsername() {
            console ? console.readLine('\nPlease specify username: ') : null
        }

        String askForPassword() {
            console ? new String(console.readPassword('\nPlease specify password: ')) : null
        }
    }
}
