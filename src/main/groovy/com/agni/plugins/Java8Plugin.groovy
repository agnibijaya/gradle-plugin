package com.agni.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to use Java
 * This will:
 * - apply java plugin
 * - set (source|target) compatibility to 1.8
 * - set compile option to 1.8
 * - will create javadoc if env var `PUBLISH_JAVADOC=true`
 * - create `testArtifact` from `src/test/java` folder
 * - add helper function `projectTestArtifact` for depending on test artifacts
 * - if 'maven-publish' plugin is applied, this will configure publications
 *  for the artifact:
 *   - groupId is the root project's name (set in settings.gradle with
 *      rootProject.name="com.agni.myApp")
 *   - artifactId is `project.archivesBaseName`
 *   - apply annotation processor plugin
 *
 */
class Java8Plugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.configure(project) {

            apply plugin: 'java-library'
            apply plugin: 'net.ltgt.apt'

            tasks.create('sourceJar', Jar) {
                group 'build'
                classifier 'sources'
                from sourceSets.main.allJava
            }
            plugins.withId('maven-publish') {
                publishing {
                    publications {
                        mavenJava(MavenPublication) {
                            from components.java
                            artifact tasks.sourceJar

                            artifactId project.archivesBaseName
                            groupId rootProject.name
                        }
                    }
                    repositories.addAll getPublishRepos()
                }
            }
            tasks.compileJava {
                sourceCompatibility = "1.8"
                targetCompatibility = "1.8"
                options.encoding = 'UTF-8'
            }
            tasks.javadoc.onlyIf { System.getenv("PUBLISH_JAVADOC") == "true" }

            // create  'testArtifact' configurations for each java project (lets us depend on one project's tests from another)
            configurations {
                testArtifact.extendsFrom testRuntime
            }

            target.tasks.create(name: 'testJar', type: Jar) {
                classifier "test"
                from sourceSets.test.output
            }
            artifacts {
                testArtifact target.tasks.testJar
            }
            def projectTestArtifact = { String projectPath ->
                dependencies.project(path: projectPath, configuration: 'testArtifact')
            }
            ext.projectTestArtifact = projectTestArtifact

        }
    }
}
