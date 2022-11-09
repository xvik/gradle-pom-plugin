package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 27.05.2021
 */
class BomRemoveKitTest extends AbstractKitTest {

    def "Check default platform behaviour"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
                implementation 'com.google.inject:guice'
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

        then: "dependency version not set"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }
        !dep.version

        then: "dependency management section remains"
        pom.dependencyManagement.dependencies.dependency.size() == 1
    }

    def "Check forced versions"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
                implementation 'com.google.inject:guice'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pomGeneration {
                forceVersions()
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

        then: "dependency version set"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }
        dep.version.text() == '4.0'

        then: "dependency management section remains"
        pom.dependencyManagement.dependencies.dependency.size() == 1
    }

    def "Check forced versions without repository declared"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
                implementation 'com.google.inject:guice'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pomGeneration {
                forceVersions()
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }

            // !!!! repositories {mavenCentral()}
        """)

        when: "run pom task"
        def res = runFailed('generatePomFileForMavenPublication')

        then: "problem detected"
        res.output.contains('No versions resolved for the following dependencies')
        res.output.contains('com.google.inject:guice')
    }

    def "Check removed platform section"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
                implementation 'com.google.inject:guice'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pomGeneration {
                removeDependencyManagement()
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

        then: "dependency version not set"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }
        dep.version.text() == '4.0'

        then: "dependency management section removed"
        !pom.dependencyManagement
    }

    def "Check dependency management plugin sections remove"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
                id "io.spring.dependency-management" version "1.0.11.RELEASE"
            }

            dependencyManagement {
                imports {
                    mavenBom 'com.google.inject:guice-bom:4.0'
                }
            }
            dependencies {
                implementation 'com.google.inject:guice'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pomGeneration {
                removeDependencyManagement()
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

        then: "dependency version set"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }
        dep.version.text() == '4.0'

        then: "dependency management section removed"
        !pom.dependencyManagement
    }
}
