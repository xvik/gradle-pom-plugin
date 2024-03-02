package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser
import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 15.02.2024
 */
class ConfigurationCacheSupportKitTest extends AbstractKitTest {

    def "Check simple pom modifications"() {
        setup:
        build("""
            plugins {
                id 'groovy'
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

            maven.pom {
                developers {
                    developer {
                        id = "dev"
                        name = "Dev Dev"
                        email = "dev@gmail.com"
                    }
                }
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run check task with both sources"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn',
                'generatePomFileForMavenPublication',)

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        
        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn',
                'generatePomFileForMavenPublication')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
    }


    def "Check all updates"() {
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
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }    
            
            maven {
                pom {
                    developers {
                        developer {
                            id = "dev"
                            name = "Dev Dev"
                            email = "dev@gmail.com"
                        }
                    }
                }   

                withPom {
                    scm {
                        url 'http://sdsds.dd'
                    }
                }   

                withPomXml {
                    asNode().appendNode('inceptionYear', 2020)
                }   
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn',
                'generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "dependencies corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'
        def opt = pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }
        opt.scope.text() == 'compile'
        opt.optional.text() == 'true'

        then: "pom modification applied"
        def developer = pom.developers.developer
        developer.id.text() == 'dev'
        developer.name.text() == 'Dev Dev'
        developer.email.text() == 'dev@gmail.com'
        pom.scm.url.text() == 'http://sdsds.dd'
        pom.inceptionYear.text() == '2020'

        then: "defaults applied"
        pom.name.text() == testProjectDir.name
        pom.description.text() == 'sample description'


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn',
                'generatePomFileForMavenPublication')

        pomFile = file("build/generated-pom.xml")
        pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        
        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        then: "dependencies corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'

        with(pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }) {
            scope.text() == 'compile'
            optional.text() == 'true'
        }

        then: "pom modification applied"
        with (pom.developers.developer) {
            id.text() == 'dev'
            name.text() == 'Dev Dev'
            email.text() == 'dev@gmail.com'
        }
        pom.scm.url.text() == 'http://sdsds.dd'
        pom.inceptionYear.text() == '2020'

        then: "defaults applied"
        pom.name.text() == testProjectDir.name
        pom.description.text() == 'sample description'
    }


    def "Check all updates with debug"() {
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
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }    
            
            maven {  
                debug()

                pom {
                    developers {
                        developer {
                            id = "dev"
                            name = "Dev Dev"
                            email = "dev@gmail.com"
                        }
                    }
                }   

                withPom {
                    scm {
                        url 'http://sdsds.dd'
                    }
                }   

                withPomXml {
                    asNode().appendNode('inceptionYear', 2020)
                }   
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn',
                'generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "dependencies corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'
        def opt = pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }
        opt.scope.text() == 'compile'
        opt.optional.text() == 'true'

        then: "pom modification applied"
        def developer = pom.developers.developer
        developer.id.text() == 'dev'
        developer.name.text() == 'Dev Dev'
        developer.email.text() == 'dev@gmail.com'
        pom.scm.url.text() == 'http://sdsds.dd'
        pom.inceptionYear.text() == '2020'

        then: "defaults applied"
        pom.name.text() == testProjectDir.name
        pom.description.text() == 'sample description'


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn',
                'generatePomFileForMavenPublication')

        pomFile = file("build/generated-pom.xml")
        pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()


        then: "cache not used"
        // no cache because of tmp file used during debug - doesn't matter
        !result.output.contains('Reusing configuration cache.')
    }
}
