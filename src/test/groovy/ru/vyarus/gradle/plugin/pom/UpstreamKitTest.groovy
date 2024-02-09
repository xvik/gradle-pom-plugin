package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2020
 */
class UpstreamKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '8.0'

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

            dependencies {                                         
                // compile
                implementation 'org.javassist:javassist:3.16.1-GA'         
                provided 'com.google.code.findbugs:annotations:3.0.0'
                // runtime
                runtimeOnly 'ru.vyarus:guice-ext-annotations:1.1.1'  
                optional 'ru.vyarus:generics-resolver:2.0.0'
                // disappear    
                compileOnly 'junit:junit:4.12'
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
        runVer(GRADLE_VERSION, 'generatePomFileForMavenPublication', '--warning-mode', 'all')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "implementation dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'

        then: "provided dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'

        then: "runtimeOnly dependency scope correct"
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'

        then: "optional dependency scope corrected"
        def opt = pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }
        opt.scope.text() == 'compile'
        opt.optional.text() == 'true'

        then: "compileOnly dependencies are removed from pom"
        pom.dependencies.'*'.find { it.artifactId.text() == 'junit' } == null

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
