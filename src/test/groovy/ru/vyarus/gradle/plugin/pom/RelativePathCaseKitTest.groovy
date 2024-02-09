package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 05.08.2016
 */
class RelativePathCaseKitTest extends AbstractKitTest {

    def "Check names clash workaround"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            maven.pom {
                parent {
                    name 'name'
                    _relativePath 'path'
                }
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "compile dependency scope corrected"
        pom.parent.relativePath.text() == 'path'

    }
}
