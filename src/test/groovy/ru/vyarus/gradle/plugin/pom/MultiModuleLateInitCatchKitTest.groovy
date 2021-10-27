package ru.vyarus.gradle.plugin.pom

import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 27.10.2021
 */
class MultiModuleLateInitCatchKitTest extends AbstractKitTest {

    def "Check incorrect pom application"() {
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

                pom {
                    description 'sample'
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
        BuildResult res = runFailed('generatePomFileForMavenPublication')

        then: "incorrect usage detected"
        res.output.contains('Pom closure declared in project \':mod\' actually applied in \':\'')
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

                withPomXml {
                    it.appendNode('tata', 'blabla')
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
        BuildResult res = runFailed('generatePomFileForMavenPublication')

        then: "incorrect usage detected"
        res.output.contains('WithPomXml closure declared in project \':mod\' actually applied in \':\'')
    }

    def "Check late pom application workaround"() {
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

                    pom {
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
        run(':mod:generatePomFileForMavenPublication')

        def pomFile = file("mod/build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "pom config applied"
        pom.description.text() == 'sample'
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

            project(':mod').pom {
                description 'sample'
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
