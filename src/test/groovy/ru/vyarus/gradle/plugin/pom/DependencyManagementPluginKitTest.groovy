package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 05.08.2016
 */
class DependencyManagementPluginKitTest extends AbstractKitTest {

    def "Check dependency management plugin compatibility"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
                id "io.spring.dependency-management" version "1.0.11.RELEASE"
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            dependencyManagement {
                dependencies {
                    dependency 'org.javassist:javassist:3.16.1-GA'
                }
            }
            dependencies {
                compile 'org.javassist:javassist'
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

        then: "compile dependency scope corrected"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }
        dep.scope.text() == 'compile'

        then: "compile dependency version not set"
        !dep.version
    }

    def "Check dependency management plugin with bom"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
                id "io.spring.dependency-management" version "1.0.11.RELEASE"
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'
            configurations.create('ttt')
            dependencyManagement {
                imports {
                    mavenBom 'com.google.inject:guice-bom:4.0'
                }
                ttt {
                    imports {
                        mavenBom 'com.google.inject:guice-bom:4.0'
                    }
                }
            }
            dependencies {
                compile 'com.google.inject:guice'
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

            repositories {mavenCentral()}
        """)

        when: "run pom task"
        run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "compile dependency scope corrected"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }
        dep.scope.text() == 'compile'

        then: "compile dependency version not set"
        !dep.version
    }


    def "Check compileOnly dependency from bom"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
                id "io.spring.dependency-management" version "1.0.11.RELEASE"
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'     

            repositories { mavenCentral(); mavenLocal() }
            dependencyManagement {                   
                dependencies {
                    dependency "junit:junit:4.12"
                }
            }
            dependencies {
                compileOnly 'junit:junit'
            }  

            pom {
                developers {
                    developer {
                        id 'jdoe'
                        name 'John Doe'
                    }
                }
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

        then: "compileOnly dependency not added"
        pom.dependencies.'*'.find { it.artifactId.text() == 'junit' } == null
    }
}
