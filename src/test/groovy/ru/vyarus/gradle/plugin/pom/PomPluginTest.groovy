package ru.vyarus.gradle.plugin.pom

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * @author Vyacheslav Rusakov 
 * @since 05.11.2015
 */
class PomPluginTest extends AbstractTest {


    def "Check no java plugin"() {

        when: "java plugin not active"
        Project project = project {
            apply plugin: "ru.vyarus.pom"
        }

        then: "configurations are not applied"
        project.configurations.findByName("provided") == null
        project.configurations.findByName("optional") == null

        then: "extension container is not registered"
        project.convention.plugins.pom == null

        then: "extension is not registered"
        project.extensions.findByName('pomGeneration') == null

        then: "maven publish plugin not activated"
        project.plugins.findPlugin(MavenPublishPlugin) == null
    }

    def "Check configurations applied"() {

        when: "java plugin active"
        Project project = project {
            apply plugin: "java"
            apply plugin: "ru.vyarus.pom"
        }

        then: "configurations registered"
        project.configurations.findByName("provided")
        project.configurations.findByName("optional")
        project.configurations.findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom
                .collect{it.name} == ["compile", "provided", "optional"]

        then: "extension container registered"
        project.convention.plugins.pom
        project.convention.plugins.pom instanceof PomConvention

        then: "extension registered"
        project.extensions.findByName('pomGeneration')
        project.extensions.findByName('pomGeneration') instanceof PomExtension

        then: "maven publish plugin activated"
        project.plugins.findPlugin(MavenPublishPlugin)
    }

    def "Check configurations applied for groovy"() {

        when: "groovy plugin active"
        Project project = project {
            apply plugin: "groovy"
            apply plugin: "ru.vyarus.pom"
        }

        then: "configurations registered"
        project.configurations.findByName("provided")
        project.configurations.findByName("optional")
        project.configurations.findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom
                .collect{it.name} == ["compile", "provided", "optional"]

        then: "extension container registered"
        project.convention.plugins.pom
        project.convention.plugins.pom instanceof PomConvention

        then: "extension registered"
        project.extensions.findByName('pomGeneration')
        project.extensions.findByName('pomGeneration') instanceof PomExtension

        then: "maven publish plugin activated"
        project.plugins.findPlugin(MavenPublishPlugin)
    }

    def "Check configurations not applied for java-library"() {

        when: "java-library plugin active"
        Project project = project {
            apply plugin: "java-library"
            apply plugin: "ru.vyarus.pom"
        }

        then: "configurations registered"
        project.configurations.findByName("provided")
        project.configurations.findByName("optional")

        then: "extension container registered"
        project.convention.plugins.pom
        project.convention.plugins.pom instanceof PomConvention

        then: "extension registered"
        project.extensions.findByName('pomGeneration')
        project.extensions.findByName('pomGeneration') instanceof PomExtension

        then: "maven publish plugin activated"
        project.plugins.findPlugin(MavenPublishPlugin)
    }
}