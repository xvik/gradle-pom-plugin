package ru.vyarus.gradle.plugin.pom

/**
 * Apt plugin adds special configuration "apt". Google gson dependency is declared directly in compile,
 * but also present as transitive in apt configuration.
 *
 * @author Vyacheslav Rusakov
 * @since 20.05.2016
 */
class ExtraConfigurationKitTest extends AbstractKitTest {

    def "Check extra configuration usage"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
                id 'net.ltgt.apt' version '0.6'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            configurations.provided.extendsFrom configurations.apt

            repositories { jcenter() }
            dependencies {
                implementation 'com.google.code.gson:gson:2.6.2'

                apt 'org.immutables:value:2.1.19'
                apt 'org.immutables:builder:2.1.19'
                apt 'org.immutables:gson:2.1.19'
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

        then: "compile dependency not overridden by provided"
        pom.dependencies.'*'.find { it.groupId.text() == 'com.google.code.gson' && it.artifactId.text() == 'gson' }.scope.text() == 'compile'

        then: "apt dependencies become provided"
        pom.dependencies.'*'.find { it.groupId.text() == 'org.immutables' && it.artifactId.text() == 'gson'  }.scope.text() == 'provided'
    }
}
