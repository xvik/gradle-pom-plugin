package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2020
 */
class CompileOnlyDuplicateKitTest extends AbstractKitTest {

    def "Check duplicate compileOnly detection"() {
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
                implementation 'org.javassist:javassist:3.16.1-GA'
                compileOnly 'org.javassist:javassist:3.16.1-GA'
                compileOnly 'com.google.code.findbugs:annotations:3.0.0'
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

        then: "implmentation dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'

        then: "compileOnly duplicate dependency not added"
        pom.dependencies.'*'.findAll { it.artifactId.text() == 'javassist' }.size() == 1

        then: "compileOnly other dependency added"
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'
    }
}
