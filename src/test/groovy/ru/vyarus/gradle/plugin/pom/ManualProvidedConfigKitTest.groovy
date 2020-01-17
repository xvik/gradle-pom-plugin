package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 18.01.2020
 */
class ManualProvidedConfigKitTest extends AbstractKitTest {

    def "Check pom modifications"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            configurations.compileOnly.extendsFrom configurations.create('provided')

            dependencies {                                                      
                provided 'com.google.code.findbugs:annotations:3.0.0'                
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pom {
                developers {
                    developer {
                        id "dev"
                        name "Dev Dev"
                        email "dev@gmail.com"
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

        then: "provided dependencies added"
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'
    }
}
