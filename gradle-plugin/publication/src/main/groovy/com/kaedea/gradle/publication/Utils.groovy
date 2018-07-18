/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.Project

/**
 * Package local utility.
 */
class Utils {

    static def isReleaseBuild(Project project) {
        readFromProperties(project, 'VERSION_NAME').contains("SNAPSHOT") == false
    }

    static def readFromProperties(Project project, String key, boolean askUser = false) {
        if (project.hasProperty(key)) return project.findProperty(key)
        if (System.getenv(key)) return System.getenv(key)
        if (!askUser) return 'unspecific'
        askUserFor(key)
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
