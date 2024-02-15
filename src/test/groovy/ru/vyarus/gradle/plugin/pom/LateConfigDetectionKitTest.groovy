package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser
import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 15.02.2024
 */
class LateConfigDetectionKitTest extends AbstractKitTest {

    def "Check late pom model update"() {
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

            afterEvaluate {
                maven.pom {
                    name = "overridden name"
                }              
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        BuildResult result = runFailed('generatePomFileForMavenPublication')

        then: "fail"
        result.output.contains("Too late maven.pom() appliance: configuration already applied")
    }

    def "Check late pom closure registration"() {
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
            
            tasks.named('generatePomFileForMavenPublication').configure {       
                doLast {
                    maven.withPom {
                        name "overridden name"
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
        BuildResult result = runFailed('generatePomFileForMavenPublication')

        then: "fail"
        result.output.contains("Too late maven.withPom() appliance: configuration already applied")
    }


    def "Check late xml closure registration"() {
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
            
            tasks.named('generatePomFileForMavenPublication').configure {       
                doLast {
                    maven.withPomXml {
                        asNode().addNode('some', 'value')
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
        BuildResult result = runFailed('generatePomFileForMavenPublication')

        then: "fail"
        result.output.contains("Too late maven.withPomXml() appliance: configuration already applied")
    }
}
