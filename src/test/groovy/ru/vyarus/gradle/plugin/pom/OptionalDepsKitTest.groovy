package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2020
 */
class OptionalDepsKitTest extends AbstractKitTest {

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
            
            java {
                registerFeature('someFeature') {
                    usingSourceSet(sourceSets.main)
                }     
            }

            dependencies {                                         
                implementation 'org.javassist:javassist:3.16.1-GA'                    

                someFeatureImplementation 'com.google.code.findbugs:annotations:3.0.0'                
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                        suppressPomMetadataWarningsFor('someFeatureApiElements')
                        suppressPomMetadataWarningsFor('someFeatureRuntimeElements')
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

        then: "implmentation dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'

        then: "some feature dependencies exists as optionals"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }
        dep.scope.text() == 'runtime'
        dep.optional.text() == 'true'
    }
}
