package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov 
 * @since 06.11.2015
 */
class PomPluginKitTest extends AbstractKitTest {

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
                // provided             
                compileOnly 'com.google.code.findbugs:annotations:3.0.0'
                // runtime
                runtimeOnly 'ru.vyarus:guice-ext-annotations:1.1.1'

                // deprecated
                compile 'ru.vyarus:gradle-pom-plugin:1.0.0'
                runtime 'ru.vyarus:gradle-quality-plugin:2.0.0'
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

        then: "implmentation dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'

        then: "compileOnly dependencies added"
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'

        then: "runtimeOnly dependency scope correct"
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'

        then: "compile dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'gradle-pom-plugin' }.scope.text() == 'compile'

        then: "runtime dependency scope corrected"
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

    def "Check pom defaults override"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pom {
                name "override"
                description "override"
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

        then: "defaults should not be applied"
        pom.name.text() == 'override'
        pom.description.text() == 'override'
    }

    def "Check description not applied if not set"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            // description 'sample description'  <-- description not set in project

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

        then: "description tag not created"
        !pom.description
    }


    def "Check multiple publications"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0

            dependencies {
                 // compile
                implementation 'org.javassist:javassist:3.16.1-GA'
                // provided             
                compileOnly 'com.google.code.findbugs:annotations:3.0.0'
                // runtime
                runtimeOnly 'ru.vyarus:guice-ext-annotations:1.1.1'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                        pom.withXml {
                            asNode().appendNode('name', 'first')
                        }
                    }

                    maven2(MavenPublication) {
                        from components.java
                        pom.withXml {
                            asNode().appendNode('name', 'second')
                        }
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
                tasks.generatePomFileForMaven2Publication {
                    destination = file("\$buildDir/generated-pom2.xml")
                }
            }
        """)

        when: "run pom task"
        run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        def developer = pom.developers.developer
        // for debug
        println pomFile.getText()

        then: "modifications applied"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'
        developer.id.text() == 'dev'
        pom.name.text() == 'first'

        when: "run pom2 task"
        run('generatePomFileForMaven2Publication')

        pomFile = file("build/generated-pom2.xml")
        pom = new XmlParser().parse(pomFile)
        developer = pom.developers.developer
        // for debug
        println pomFile.getText()

        then: "modifications applied"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'
        developer.id.text() == 'dev'
        pom.name.text() == 'second'
    }

    def "Check test dependencies removed"() {
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
                testCompile 'com.google.code.findbugs:annotations:3.0.0'
                testRuntime 'ru.vyarus:generics-resolver:2.0.0'
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

        then: "test dependency remain"
        !pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }
        !pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }
    }
}