package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser

/**
 * @author Vyacheslav Rusakov
 * @since 19.05.2021
 */
class PlatformKitTest extends AbstractKitTest {

    def "Check activation for java-platform"() {
        setup:
        build("""
            plugins {
                id 'java-platform'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            javaPlatform {
                allowDependencies()
            }

            dependencies {
                api platform('ru.vyarus.guicey:guicey-bom:5.2.0-1')
                constraints {
                    api 'org.pf4j:pf4j:3.6.0'
                    api 'org.pf4j:pf4j-update:2.3.0'
                }
            }

            publishing {
                publications {
                    BOM(MavenPublication) {
                        from components.javaPlatform
                    }
                }
            }

            model {
                tasks.generatePomFileForBOMPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        run('generatePomFileForBOMPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "test dependency remain"
        pom.dependencyManagement.dependencies.'*'.find { it.artifactId.text() == 'guicey-bom' }.scope.text() == 'import'
        def dep = pom.dependencyManagement.dependencies.'*'.find { it.artifactId.text() == 'pf4j' }
        dep != null
        !dep.scope || dep.scope.text() == 'compile' // depends on gradle version

        then: "check dependencies order"
        unifyString(pomFile.text).contains """<dependencies>
      <dependency>
        <groupId>ru.vyarus.guicey</groupId>
        <artifactId>guicey-bom</artifactId>
        <version>5.2.0-1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.pf4j</groupId>
        <artifactId>pf4j</artifactId>
        <version>3.6.0</version>
      </dependency>
      <dependency>
        <groupId>org.pf4j</groupId>
        <artifactId>pf4j-update</artifactId>
        <version>2.3.0</version>
      </dependency>
    </dependencies>"""
    }


    def "Check java-platform default behavior"() {
        setup:
        build("""
            plugins {
                id 'java-platform'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            javaPlatform {
                allowDependencies()
            }

            dependencies {
                api platform('ru.vyarus.guicey:guicey-bom:5.2.0-1')
                constraints {
                    api 'org.pf4j:pf4j:3.6.0'
                    api 'org.pf4j:pf4j-update:2.3.0'
                }
            }

            maven {
                disableBomsReorder()
            }

            publishing {
                publications {
                    BOM(MavenPublication) {
                        from components.javaPlatform
                    }
                }
            }

            model {
                tasks.generatePomFileForBOMPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        run('generatePomFileForBOMPublication')

        def pomFile = file("build/generated-pom.xml")
        // for debug
        println pomFile.getText()

        then: "check dependencies order"
        unifyString(pomFile.text).contains """<dependencies>
      <dependency>
        <groupId>org.pf4j</groupId>
        <artifactId>pf4j</artifactId>
        <version>3.6.0</version>
      </dependency>
      <dependency>
        <groupId>org.pf4j</groupId>
        <artifactId>pf4j-update</artifactId>
        <version>2.3.0</version>
      </dependency>
      <dependency>
        <groupId>ru.vyarus.guicey</groupId>
        <artifactId>guicey-bom</artifactId>
        <version>5.2.0-1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>"""
    }
}