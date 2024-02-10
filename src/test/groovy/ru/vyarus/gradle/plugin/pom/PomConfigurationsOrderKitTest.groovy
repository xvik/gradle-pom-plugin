package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 10.02.2024
 */
class PomConfigurationsOrderKitTest extends AbstractKitTest {

    def "Check pom modifications order"() {
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

                        pom {
                           println 'publication pom'
                        }

                        pom.withXml {
                           println 'publication withXml' 
                        }
                    }
                }
            }
            
            maven {
                pom {
                    println 'plugin pom'                
                }
                withPom {              
                    println 'plugin withPom'
                }
                withPomXml {
                    println 'plugin withPomXml'
                }
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        def res = run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "defaults applied"
        res.output.contains("""> Configure project :
publication pom
plugin pom

> Task :generatePomFileForMavenPublication
publication withXml
plugin withPom
plugin withPomXml""")
    }

}
