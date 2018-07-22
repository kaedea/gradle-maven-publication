/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.*
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

import static com.kaedea.gradle.publication.Extension.BINTRAY_API_KEY
import static com.kaedea.gradle.publication.Extension.BINTRAY_NAME
import static com.kaedea.gradle.publication.Extension.BINTRAY_REPO
import static com.kaedea.gradle.publication.Extension.BINTRAY_USERNAME
import static com.kaedea.gradle.publication.Extension.GROUP
import static com.kaedea.gradle.publication.Extension.NEXUS_PASSWORD
import static com.kaedea.gradle.publication.Extension.NEXUS_USERNAME
import static com.kaedea.gradle.publication.Extension.POM_ARTIFACT_ID
import static com.kaedea.gradle.publication.Extension.POM_DESCRIPTION
import static com.kaedea.gradle.publication.Extension.POM_DEVELOPER_ID
import static com.kaedea.gradle.publication.Extension.POM_DEVELOPER_NAME
import static com.kaedea.gradle.publication.Extension.POM_LICENCE_DIST
import static com.kaedea.gradle.publication.Extension.POM_LICENCE_NAME
import static com.kaedea.gradle.publication.Extension.POM_LICENCE_URL
import static com.kaedea.gradle.publication.Extension.POM_NAME
import static com.kaedea.gradle.publication.Extension.POM_PACKAGING
import static com.kaedea.gradle.publication.Extension.POM_SCM_CONNECTION
import static com.kaedea.gradle.publication.Extension.POM_SCM_DEV_CONNECTION
import static com.kaedea.gradle.publication.Extension.POM_SCM_URL
import static com.kaedea.gradle.publication.Extension.POM_URL
import static com.kaedea.gradle.publication.Extension.RELEASE_REPOSITORY_URL
import static com.kaedea.gradle.publication.Extension.SNAPSHOT_REPOSITORY_URL
import static com.kaedea.gradle.publication.Extension.VERSION_NAME

/**
 * Custom gradle plugin that helps to apply the gradle publishing plugin {@link MavenPlugin}.
 * @see "https://docs.gradle.org/current/userguide/maven_plugin.html"
 */
class PublicationPlugin implements Plugin<Project> {

    def project
    def extension

    def uploadTaskPath = {
        project.rootProject == project ?
                ":uploadArchives" :
                "$project.path:uploadArchives"
    }

    def opt = { extension.get(it) ?: Utils.readFromProperties(project, it) }
    def must = { extension.get(it) ?: Utils.readFromPropertiesVital(project, it) }

    def repositoryUsername = {
        extension.get(NEXUS_USERNAME) ?:
                Utils.readFromProperties(project, NEXUS_USERNAME, true)
    }
    def repositoryPassword = {
        extension.get(NEXUS_PASSWORD) ?:
                Utils.readFromProperties(project, NEXUS_PASSWORD, true)
    }

    @Override
    void apply(Project project) {
        this.project = project
        this.extension = project.extensions.create('publication', Extension)

        project.logger.lifecycle "----------"
        project.logger.lifecycle "Publication: apply gradle maven publishing tasks..."
        project.logger.lifecycle "----------"

        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        configureIdentifier()
        configureArtifactTasks()
        configurePom()
        configureUpload()
        configureSigning()
        configureBintray()
    }

    private void configureIdentifier() {
        project.afterEvaluate {
            project.group = must(GROUP)
            project.version = must(VERSION_NAME)
            // project.name is read-only thus nothing we can do here
        }
    }

    private void configureArtifactTasks() {
        project.afterEvaluate {
            configureSourcesJarTask()
            configureJavadocJarTask()
            configureTestsJarTask()

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

            if (Utils.isAndroidProject(project)) {
                if (extension.jarSources) {
                    addArtifactTask("androidSourcesJar")
                }
                if (extension.jarJavaDoc) {
                    addArtifactTask("androidJavadocJar")
                }
                if (extension.jarTests) {
                    addArtifactTask("androidTestsJar")
                    addArtifactTask("testsJar")
                }
                // Kotlin
                // @see "https://github.com/Kotlin/dokka"
            } else {
                if (extension.jarSources) {
                    addArtifactTask("sourcesJar")
                }
                if (extension.jarJavaDoc) {
                    addArtifactTask("javadocJar")
                }
                if (extension.jarTests) {
                    addArtifactTask("testsJar")
                }
                // Groovy
                // Kotlin
            }
        }
    }

    private void configureSourcesJarTask() {
        if (Utils.isAndroidProject(project)) {
            project.task('androidSourcesJar', type: Jar) {
                classifier = 'sources'
                group = 'build'
                description = 'Assemble a jar archive containing the main sources.'
                from project.android.sourceSets.main.java.source
            }
            project.android.libraryVariants.all { variant ->
                def name = variant.name
                project.task("jar${name.capitalize()}", type: Jar, dependsOn: variant.javaCompile) {
                    from variant.javaCompile.destinationDir
                }
                if (name == 'release') {
                    def scope = variant.variantData.scope
                    project.androidSourcesJar.dependsOn scope.javacTask.name
                    project.androidSourcesJar.from scope.annotationProcessorOutputDir, scope.buildConfigSourceOutputDir
                }
            }
        } else {
            project.task('sourcesJar', type: Jar) {
                classifier = 'sources'
                group = 'build'
                description = 'Assemble a jar archive containing the main sources.'
                from project.sourceSets.main.allSource
            }
        }
    }

    private void configureJavadocJarTask() {
        if (Utils.isAndroidProject(project)) {
            project.task('androidJavadoc', type: Javadoc) {
                group = 'build'
                description = 'Generated Android Javadoc API documentation.'
                source = project.android.sourceSets.main.java.source
                exclude '**/R.html'
                exclude '**/R.*.html'
                exclude '**/index.html'
                exclude '**/pom.xml'
                exclude '**/proguard_annotations.pro'
                classpath += project.files(project.android.bootClasspath.join(File.pathSeparator))
                project.android.libraryVariants.all { variant ->
                    if (variant.name == 'release') {
                        owner.classpath += variant.javaCompile.classpath
                    }
                }
            }
            project.task('androidJavadocJar', type: Jar, dependsOn: 'androidJavadoc') {
                classifier = 'javadoc'
                group = 'build'
                description = 'Assemble a jar archive containing the generated Javadoc API documentation.'
                from project.androidJavadoc.destinationDir
            }
        } else {
            project.task('javadocJar', type: Jar) {
                classifier = 'javadoc'
                group = 'build'
                description = 'Assemble a jar archive containing the generated Javadoc API documentation.'
                from project.plugins.hasPlugin(GroovyPlugin) ?
                        project.tasks.getByName(GroovyPlugin.GROOVYDOC_TASK_NAME) :
                        project.tasks.getByName(JavaPlugin.JAVADOC_TASK_NAME)
            }
        }
    }

    private void configureTestsJarTask() {
        if (Utils.isAndroidProject(project)) {
            project.task('androidTestsJar', type: Jar) {
                classifier = 'androidTests'
                group = 'build'
                description = 'Assemble a jar archive containing the androidTest sources.'
                from project.android.sourceSets.androidTest.java.source // Not sure here
            }
            project.task('testsJar', type: Jar) {
                classifier = 'tests'
                group = 'build'
                description = 'Assemble a jar archive containing the test sources.'
                from project.android.sourceSets.test.java.source // Not sure here
            }
        } else {
            project.task('testsJar', type: Jar) {
                classifier = 'tests'
                group = 'build'
                description = 'Assemble a jar archive containing the test sources.'
                from project.sourceSets.test.output
            }
        }
    }

    private void addArtifactTask(String taskName) {
        Task task = project.tasks.findByName(taskName)
        if (task) {
            project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task)
        } else {
            throw new GradleException("Can not find task, name = $taskName")
        }
    }

    private void configurePom() {
        project.afterEvaluate {
            project.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                pom.project {
                    groupId must(GROUP)
                    artifactId opt(POM_ARTIFACT_ID) ?: project.name
                    version must(VERSION_NAME)

                    name opt(POM_NAME) ?: project.name
                    packaging opt(POM_PACKAGING) ?: Utils.isAndroidProject(project) ? 'aar' : 'jar'
                    url opt(POM_URL)
                    description opt(POM_DESCRIPTION)

                    scm {
                        url opt(POM_SCM_URL)
                        connection opt(POM_SCM_CONNECTION)
                        developerConnection opt(POM_SCM_DEV_CONNECTION)
                    }
                    licenses {
                        license {
                            name opt(POM_LICENCE_NAME)
                            url opt(POM_LICENCE_URL)
                            distribution opt(POM_LICENCE_DIST)
                        }
                    }
                    developers {
                        developer {
                            id opt(POM_DEVELOPER_ID)
                            name opt(POM_DEVELOPER_NAME)
                        }
                    }
                }

                def scopeMappings = pom.scopeMappings
                def addDependency = { configuration, scope ->
                    if (configuration != null) scopeMappings.addMapping(1, configuration, scope)
                }
                if (Utils.isAndroidProject(project)) {
                    addDependency(project.configurations.provided, 'provided')
                    addDependency(project.configurations.compileOnly, 'provided')
                    addDependency(project.configurations.androidTestCompile, 'test')
                    addDependency(project.configurations.androidTestApi, 'test')
                    addDependency(project.configurations.androidTestImplementation, 'test')
                    addDependency(project.configurations.testCompile, 'test')
                    addDependency(project.configurations.testApi, 'test')
                    addDependency(project.configurations.testImplementation, 'test')
                } else {
                    addDependency(project.configurations.implementation, 'compile')
                    addDependency(project.configurations.compileOnly, 'provided')
                    addDependency(project.configurations.runtimeOnly, 'runtime')
                }
            }
        }
    }

    private void configureUpload() {
        project.afterEvaluate {
            project.tasks.getByName("uploadArchives").repositories.mavenDeployer() {
                project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if (taskGraph.hasTask(uploadTaskPath())) {

                        if (!opt(RELEASE_REPOSITORY_URL) && !opt(SNAPSHOT_REPOSITORY_URL)) {
                            // publish to local maven
                            repository(url: project.uri(project.rootProject.file('maven')))
                        }

                        if (opt(RELEASE_REPOSITORY_URL)) {
                            repository(url: opt(RELEASE_REPOSITORY_URL)) {
                                authentication(
                                        userName: repositoryUsername(),
                                        password: repositoryPassword()
                                )
                            }
                        }
                        if (opt(SNAPSHOT_REPOSITORY_URL)) {
                            snapshotRepository(url: opt(SNAPSHOT_REPOSITORY_URL)) {
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
        if (!extension.signing)
            return

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
            }
        }
        project.signing {
            required {
                Utils.isReleaseBuild(project) && project.gradle.taskGraph.hasTask("uploadArchives")
            }
            sign project.configurations.archives
        }
    }

    private void configureBintray() {
        project.afterEvaluate {
            if (extension.uploadToBintray) {
                project.plugins.apply('com.jfrog.bintray')
                project.bintray {
                    user = bintrayUsername()
                    key = bintrayApiKey()
                    configurations = ['archives']

                    pkg {
                        repo = opt(BINTRAY_REPO) ?: 'repo'
                        name = opt(BINTRAY_NAME) ?: opt(POM_ARTIFACT_ID) ?: project.name
                        desc = opt(POM_DESCRIPTION)
                        websiteUrl = opt(POM_URL)
                        issueTrackerUrl = getBintrayIssueTrackerUrl()
                        vcsUrl = getBintrayVcsUrl()
                        licenses = [opt(POM_LICENCE_NAME)]
                        labels = Utils.isAndroidProject(project) ? ['aar', 'android'] : ['jar', 'java']
                        publicDownloadNumbers = true
                    }
                }
            }
        }
    }

    def getBintrayIssueTrackerUrl = { ->
        def url = opt(POM_URL)
        if (url) {
            if (url.startsWith("https://github.com") || url.startsWith("http://github.com")) {
                if (!url.endsWith("/")) url += "/"
                return url + "issues"
            }
        }
    }

    def getBintrayVcsUrl = { ->
        def url = opt(POM_URL)
        if (url) {
            if (url.startsWith("https://github.com") || url.startsWith("http://github.com")) {
                if (url.endsWith("/")) url = url.substring(0, url.length() - 2)
                return url + ".git"
            }
        }
    }

    def bintrayUsername = {
        extension.get(BINTRAY_USERNAME) ?:
                Utils.readFromProperties(project, BINTRAY_USERNAME, true)
    }

    def bintrayApiKey = {
        extension.get(BINTRAY_API_KEY) ?:
                Utils.readFromProperties(project, BINTRAY_API_KEY, true)
    }
}
