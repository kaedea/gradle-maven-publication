/*
 * Copyright (c) 2018. Kaede<kidhaibara@gmail.com>
 */

package com.kaedea.gradle.publication

import org.gradle.api.Plugin
import org.gradle.api.Project

class PublicationPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.logger.lifecycle "----------"
        project.logger.lifecycle "Publication: apply gradle maven publishing tasks..."
        project.logger.lifecycle "----------"

        // TODO: Apply maven plugins here
    }
}
