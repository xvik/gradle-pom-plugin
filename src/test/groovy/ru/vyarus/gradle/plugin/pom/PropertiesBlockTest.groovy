package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2016
 */
class PropertiesBlockTest extends AbstractKitTest {

    def "Check pom properties section"() {
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

            ext {
                some = 'name'
                libVersion = '1.0'
            }

            pom {
                properties {
                    sample some
                    'some.ver' 'tt'
                    'lib.version' libVersion
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

        then: "valid properties section"
        pom.properties.sample.text() == 'name'
        pom.properties.'some.ver'.text() == 'tt'
        pom.properties.'lib.version'.text() == '1.0'

    }
}
