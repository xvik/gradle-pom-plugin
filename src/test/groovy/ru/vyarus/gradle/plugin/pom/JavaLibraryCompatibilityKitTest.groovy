package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 11.08.2017
 */
class JavaLibraryCompatibilityKitTest extends AbstractKitTest {

    def "Check pom modifications"() {
        setup:
        build("""
            plugins {
                id 'java-library'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            dependencies {
                api 'ru.vyarus:gradle-pom-plugin:1.0.0'
                implementation 'ru.vyarus:gradle-quality-plugin:2.0.0'
                runtimeOnly 'ru.vyarus:guice-ext-annotations:1.1.1'
                compileOnly 'org.javassist:javassist:3.16.1-GA'
                provided 'junit:junit:4.12'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            maven.pom {
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

        then: "api dependency scope valid"
        pom.dependencies.'*'.find { it.artifactId.text() == 'gradle-pom-plugin' }.scope.text() == 'compile'

        then: "implementation dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'gradle-quality-plugin' }.scope.text() == 'compile'

        then: "runtimeOnly dependency scope valid"
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'

        then: "compileOnly dependency removed"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' } == null

        then: "provided dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'junit' }.scope.text() == 'provided'

        then: "pom modification applied"
        def developer = pom.developers.developer
        developer.id.text() == 'dev'
        developer.name.text() == 'Dev Dev'
        developer.email.text() == 'dev@gmail.com'

        then: "defaults applied"
        pom.name.text() != null
        pom.description.text() == 'sample description'
    }
}
