package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2020
 */
class NotStandardDependencyKitTest extends AbstractKitTest {

    def "Check indirect dependency"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            dependencies {                                                         
                implementation gradleApi()                                
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
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

        then: "no dependencies"
        pom.dependencies.size() == 0
    }

    def "Check indirect provided dependency"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            dependencies {                                                         
                provided gradleApi()                                
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
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

        then: "no dependencies"
        pom.dependencies.size() == 0
    }

    def "Check project dependency"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            description 'sample description'

            allprojects {
                group 'ru.vyarus'
                version 1.0
            }                                 

            subprojects {
                apply plugin: 'java'
            }

            dependencies {                                                         
                implementation project(':mod')                                
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }           

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)
        file('settings.gradle') << "include 'mod'"

        when: "run pom task"
        run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "valid dependency"
        pom.dependencies.'*'.find { it.artifactId.text() == 'mod' }.scope.text() == 'compile'
    }

    def "Check project provided dependency"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            description 'sample description'

            allprojects {
                group 'ru.vyarus'
                version 1.0
            }                                 

            subprojects {
                apply plugin: 'java'
            }

            dependencies {                                                         
                provided project(':mod')                                
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }           

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)
        file('settings.gradle') << "include 'mod'"

        when: "run pom task"
        run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "valid dependency"
        pom.dependencies.'*'.find { it.artifactId.text() == 'mod' }.scope.text() == 'provided'
    }
}
