/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * Custom gradle plugin that helps to apply the gradle publishing plugin {@link MavenPlugin}.
 * @see "https://docs.gradle.org/current/userguide/maven_plugin.html"
 */
class PublicationPlugin implements Plugin<Project> {

    private Project mProject

    @Override
    void apply(Project project) {
        project.logger.lifecycle "----------"
        project.logger.lifecycle "Publication: apply gradle maven publishing tasks..."
        project.logger.lifecycle "----------"

        mProject = project
        project.plugins.apply(MavenPlugin)
        project.group = project.GROUP
        project.version = project.VERSION_NAME

        configureArtifactTasks()
        configureSigning()
        configurePom()
        configureUpload()
    }

    private void configureArtifactTasks() {
        mProject.afterEvaluate {
            mProject.plugins.withType(JavaPlugin) {
                configureSourcesJarTask()
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

    private void addArtifactTask(String taskName) {
        Task task = mProject.tasks.findByName(taskName)
        if (task) {
            mProject.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task)
        }
    }

    private void configureSigning() {

    }

    private void configurePom() {

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

    String getUploadTaskPath() {
        mProject.rootProject == mProject ? ":uploadArchives" : "$mProject.path:uploadArchives"
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
