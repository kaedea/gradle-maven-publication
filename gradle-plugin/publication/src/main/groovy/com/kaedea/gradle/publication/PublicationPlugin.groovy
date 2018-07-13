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

class PublicationPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.logger.lifecycle "----------"
        project.logger.lifecycle "Publication: apply gradle maven publishing tasks..."
        project.logger.lifecycle "----------"

        project.plugins.apply(MavenPlugin)
        project.group = project.GROUP
        project.version = project.VERSION_NAME

        configureArtifactTasks(project)
        configureSigning(project)
        configurePom(project)
        configureUpload(project)
    }

    private void configureArtifactTasks(Project project) {
        project.afterEvaluate {
            project.plugins.withType(JavaPlugin) {
                configureSourcesJarTask(project)
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

            addArtifactTask(project, "sourcesJar")
        }
    }

    private void configureSourcesJarTask(Project project) {
        project.task('sourcesJar', type: Jar) {
            classifier = 'sources'
            group = "build"
            description = 'Assembles a jar archive containing the main sources of this project.'
            from project.sourceSets.main.allSource
        }
    }

    private void addArtifactTask(Project project, String taskName) {
        Task task = project.tasks.findByName(taskName)
        if (task) {
            project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task)
        }
    }

    private void configureSigning(Project project) {

    }

    private void configurePom(Project project) {

    }

    private void configureUpload(Project project) {
        project.afterEvaluate {
            project.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if (taskGraph.hasTask(getUploadTaskPath(project))) {

                        if (!getReleaseRepositoryUrl(project) && !getSnapshotRepositoryUrl(project)) {
                            // publish to local maven
                            repository(url: project.uri(project.rootProject.file('maven')))
                        }

                        if (getReleaseRepositoryUrl(project)) {
                            repository(url: getReleaseRepositoryUrl(project)) {
                                authentication(
                                        userName: getRepositoryUsername(project),
                                        password: getRepositoryPassword(project)
                                )
                            }
                        }
                        if (getSnapshotRepositoryUrl(project)) {
                            snapshotRepository(url: getSnapshotRepositoryUrl(project)) {
                                authentication(
                                        userName: getRepositoryUsername(project),
                                        password: getRepositoryPassword(project)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    String getUploadTaskPath(Project project) {
        project.rootProject == project ? ":uploadArchives" : "$project.path:uploadArchives"
    }

    def getReleaseRepositoryUrl(Project project) {
        return project.hasProperty('RELEASE_REPOSITORY_URL') ?
                project.RELEASE_REPOSITORY_URL :
                System.env.RELEASE_REPOSITORY_URL
    }

    def getSnapshotRepositoryUrl(Project project) {
        return project.hasProperty('SNAPSHOT_REPOSITORY_URL') ?
                project.SNAPSHOT_REPOSITORY_URL :
                System.env.RELEASE_REPOSITORY_URL
    }

    def getRepositoryUsername(Project project) {
        def var = project.hasProperty('NEXUS_USERNAME') ?
                project.NEXUS_USERNAME :
                System.env.NEXUS_USERNAME
        if (!var) {
            ConsoleHandler consoleHandler = new ConsoleHandler()
            var = consoleHandler.askForUsername()
        }
        return var
    }

    def getRepositoryPassword(Project project) {
        def var = project.hasProperty('NEXUS_PASSWORD') ?
                project.NEXUS_PASSWORD :
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
