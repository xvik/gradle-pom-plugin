package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 28.07.2016
 */
class MultipleDevelopersKitTest extends AbstractKitTest {

    def "Check multiple developers"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            maven.withPom {
                developers {
                    developer {
                        id "dev1"
                        name "Dev1"
                        email "dev@dev1.com"
                    }
                    developer {
                        id "dev2"
                        name "Dev2"
                        email "dev2@dev2.com"
                    }
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

        then: "both developers printed"
        pom.developers.developer.size() == 2
        pom.developers.'*'.find { it.id.text() == 'dev1' }
        pom.developers.'*'.find { it.id.text() == 'dev2' }
    }

}