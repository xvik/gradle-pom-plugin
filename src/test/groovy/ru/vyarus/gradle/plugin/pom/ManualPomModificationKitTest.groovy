package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 04.09.2016
 */
class ManualPomModificationKitTest extends AbstractKitTest {

    def "Check pom manual modification"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
            
            maven {
                pom {
                    name = "override"
                }
    
                withPomXml {
                    asNode().appendNode('tata', 'blabla')
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

        then: "all pom customizations applied"
        pom.name.text() == 'override'
        pom.tata.text() == 'blabla'
    }

    def "Check pom manual modification with multiple sections"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
            
            maven {
                pom {
                    name = "override"
                }
    
                withPomXml {
                    asNode().appendNode('tata', 'blabla')
                }
    
                withPomXml {
                    asNode().appendNode('baba', 'ablabl')
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

        then: "all pom customizations applied"
        pom.name.text() == 'override'
        pom.tata.text() == 'blabla'
        pom.baba.text() == 'ablabl'
    }

}