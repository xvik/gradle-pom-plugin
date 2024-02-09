package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 09.11.2022
 */
class RepositoriesInSettingsKitTest extends AbstractKitTest {

    def "Check forced versions with repositories in settings"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
                implementation 'com.google.inject:guice'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            maven {
                forceVersions()
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }

            // !!!! repositories {mavenCentral()}
        """)
        file('settings.gradle') << """
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
"""

        when: "run pom task"
        run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "dependency version set"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }
        dep.version.text() == '4.0'

        then: "dependency management section remains"
        pom.dependencyManagement.dependencies.dependency.size() == 1
    }

    def "Check no error when no deps"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            maven {
                forceVersions()
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }

            // !!!! repositories {mavenCentral()}
        """)
        when: "run pom task"
        def res = run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "ok"
        res.task(':generatePomFileForMavenPublication').outcome == TaskOutcome.SUCCESS
    }
}
