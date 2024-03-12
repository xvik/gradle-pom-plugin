package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser
import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 12.03.2024
 */
class MultiModulePomUnificationKitTest extends AbstractKitTest {

    def "Check incorrect pom application"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            description = 'sample description'

            allprojects {
                group 'ru.vyarus'
                version 1.0

                apply plugin: 'ru.vyarus.pom'

                maven.pom {
                    developers {
                        developer {
                            id = 'test'
                            name = 'Test'
                        }
                    }
                }                                
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
        file('settings.gradle') << "include 'mod'\nrootProject.name = 'test'"

        when: "run pom task"
        BuildResult res = run('generatePomFileForMavenPublication')
        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "incorrect root pom"
        pom.developers.developer.size() == 2
    }

    def "Check allprojects fix"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            description = 'sample description'

            allprojects {
                group 'ru.vyarus'
                version 1.0

                apply plugin: 'ru.vyarus.pom'
                
                plugins.withId("java") {
                    maven.pom {
                        developers {
                            developer {
                                id = 'test'
                                name = 'Test'
                            }
                        }
                    }                                      
                }
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
        file('settings.gradle') << "include 'mod'\nrootProject.name = 'test'"

        when: "run pom task"
        BuildResult res = run('generatePomFileForMavenPublication')
        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "root pom valid"
        pom.developers.developer.size() == 1
    }

    def "Check incorrect withPomXml application"() {
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

                apply plugin: 'ru.vyarus.pom'

                maven.withPomXml {
                    asNode().appendNode('tata', 'blabla')
                }
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
        file('settings.gradle') << "include 'mod'\nrootProject.name = 'test'"

        when: "run pom task"
        BuildResult res = run('generatePomFileForMavenPublication')
        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "incorrect root pom"
        pom.tata.size() == 2
    }

    def "Check late pom application workaround does not work"() {
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

                apply plugin: 'ru.vyarus.pom'

                afterEvaluate {
                    publishing {
                        publications {
                            maven(MavenPublication) {
                                from components.java
                            }
                        }
                    } 

                    maven.pom {
                        description 'sample'
                    }
                    
                     model {
                        tasks.generatePomFileForMavenPublication {
                            destination = file("\$buildDir/generated-pom.xml")
                        }
                    }
                }
            }                                 

            subprojects {
                apply plugin: 'java'
            }          
        """)
        file('settings.gradle') << "include 'mod'\nrootProject.name = 'test'"

        when: "run pom task"
        BuildResult res = runFailed(':mod:generatePomFileForMavenPublication')

        then: "too late detection"
        res.output.contains('Too late maven.pom() appliance: configuration already applied')
    }

    def "Check direct module configuration"() {
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

                apply plugin: 'ru.vyarus.pom'
                apply plugin: 'java'

                afterEvaluate {
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
                }
            }

            project(':mod').maven.pom {
                description = 'sample'
            }                                          
        """)
        file('settings.gradle') << "include 'mod'\nrootProject.name = 'test'"

        when: "run pom task"
        run(':mod:generatePomFileForMavenPublication')

        def pomFile = file("mod/build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "pom config applied"
        pom.description.text() == 'sample'
    }

}

