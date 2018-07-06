package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 06.07.2018
 */
class UnstablePublichingModeTest extends AbstractKitTest {

    def "Check pom modifications for 4.6"() {
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
                provided 'com.google.code.findbugs:annotations:3.0.0'
                optional 'ru.vyarus:generics-resolver:2.0.0'
                runtime 'ru.vyarus:guice-ext-annotations:1.1.1'
                compile 'org.javassist:javassist:3.16.1-GA'
                
                compileOnly 'ru.vyarus:gradle-pom-plugin:1.0.0'
                runtimeOnly 'ru.vyarus:gradle-quality-plugin:2.0.0'
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
        runVer('4.6','generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "runtime dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'

        then: "compile dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'

        then: "provided dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'

        then: "optional dependency scope corrected"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }
        dep.scope.text() == 'compile'
        dep.optional.text() == 'true'

        then: "compileOnly dependencies are removed from pom"
        pom.dependencies.'*'.find { it.artifactId.text() == 'gradle-pom-plugin' } == null

        then: "runtimeOnly dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'gradle-quality-plugin' }.scope.text() == 'runtime'

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
