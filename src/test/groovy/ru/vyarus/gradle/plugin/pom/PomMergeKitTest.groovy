package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov 
 * @since 02.12.2015
 */
class PomMergeKitTest extends AbstractKitTest {

    def "Check pom node merge"() {
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
                        // applied after maven.pom!
                        pom.withXml {
                            asNode().appendNode('name', 'first')
                            asNode().appendNode('description', 'second')
                        }
                    }
                }
            }

            maven.pom {
                name = 'custom'
            }  

            maven.withPom {
                description 'custom2'
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

        then: "pom node overridden"
        pom.name.text() == 'customfirst'
        pom.description.text() == 'custom2'
    }

    def "Check pom dsl merge (4.8)"() {
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
                            name = 'first'     
                            description = 'second'
                            scm {
                              url = "http://subversion.example.com/svn/project/trunk/"  
                              tag = "tag" 
                            }
                        }
                    }
                }
            }

            maven.pom {
                name = 'custom'
                scm {
                  url = "http://google.com"
                }
            }  

            maven.withPom {
                description 'custom2'
                scm {
                  tag "tag2"
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

        then: "pom node overridden"
        pom.name.text() == 'custom'
        pom.description.text() == 'custom2'
        pom.scm.url.text() == 'http://google.com'
        pom.scm.tag.text() == 'tag2'
    }

    def "Check pom tree recognition modifications"() {
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
                        pom.withXml {
                            def first = asNode().appendNode('first')
                            first.appendNode('cust1', 'cust11')
                            first.appendNode('second').appendNode('val1', 'custt')
                        }
                    }
                }
            }

            maven.withPom {
                first {
                    second {
                        val1 '1'
                        val2 '2'
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

        then: "pom node overridden"
        pom.first.cust1.text() == 'cust11'
        pom.first.second.val1.text() == '1'
        pom.first.second.val2.text() == '2'
    }
}