package io.codearte.accurest.plugin

import io.coderate.accurest.TestGenerator
import io.coderate.accurest.config.AccurestConfigProperties
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Jakub Kubrynski
 */
class AccurestGradlePlugin implements Plugin<Project> {

	private static final String TASK_NAME = 'generateAccurest'

	private static final Class IDEA_PLUGIN_CLASS = org.gradle.plugins.ide.idea.IdeaPlugin

	@Override
	void apply(Project project) {
		AccurestConfigProperties extension = project.extensions.create('accurest', AccurestConfigProperties)

		project.compileTestGroovy.dependsOn(TASK_NAME)

		project.task(TASK_NAME) << {
			project.logger.info("Accurest Plugin: Invoking test sources generation")

			extension.stubsBaseDirectory = project.projectDir.path + File.separator + extension.stubsBaseDirectory
			extension.generatedTestSourcesDir = buildGeneratedSourcesDir(project, extension)

			project.sourceSets.test.groovy {
				project.logger.info("Registering $extension.generatedTestSourcesDir as test source directory")
				srcDir extension.generatedTestSourcesDir
			}

			try {
				TestGenerator generator = new TestGenerator(extension)
				int generatedClasses = generator.generate()
				project.logger.info("Generated {} test classes", generatedClasses)
			} catch (IllegalStateException e) {
				project.logger.error("Accurest Plugin: {}", e.getMessage())
			}
		}

		project.afterEvaluate {
			def hasIdea = project.plugins.findPlugin(IDEA_PLUGIN_CLASS)
			if (hasIdea) {
				project.idea {
					module {
						testSourceDirs += new File(buildGeneratedSourcesDir(project, extension))
					}
				}
			}
		}

	}

	private String buildGeneratedSourcesDir(Project project, def extension) {
		String moduleDir
		if (project.getParent()) {
			moduleDir = project.name + File.separator + extension.generatedTestSourcesDir
		} else {
			moduleDir = extension.generatedTestSourcesDir
		}
		moduleDir
	}

}
