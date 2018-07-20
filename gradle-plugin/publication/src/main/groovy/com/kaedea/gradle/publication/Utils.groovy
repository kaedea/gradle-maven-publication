/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Package local utility.
 */
class Utils {

    static def isAndroidProject(Project project) {
        def plugins = project.getPlugins()
        plugins.hasPlugin('com.android.application') || plugins.hasPlugin('com.android.library')
    }


    static def isReleaseBuild(Project project) {
        def version = project.version ?
                project.version :
                readFromPropertiesVital(project, 'VERSION_NAME')
        version.contains('SNAPSHOT') == false
    }

    static def readFromProperties(Project project, String key, boolean askUser = false) {
        if (project.hasProperty(key)) return project.findProperty(key)
        if (System.getenv(key)) return System.getenv(key)
        if (!askUser) return null
        askUserFor(key)
    }

    static def readFromPropertiesVital(Project project, String key) {
        if (project.hasProperty(key)) return project.findProperty(key)
        if (System.getenv(key)) return System.getenv(key)
        throw new GradleException("Property $key is not yet configured in project or system.env!")
    }

    static def askUserFor(String key) {
        new ConsoleHandler().ask(key)
    }

    static class ConsoleHandler {
        Console console = System.console()
        def ask = { key ->
            console ? console.readLine("\nPlease specify $key: ") : null
        }
    }
}
