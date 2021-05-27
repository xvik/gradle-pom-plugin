package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 18.01.2020
 */
class GradleNativeBOMKitTest extends AbstractKitTest {

    def "Check compileOnly dependency from bom"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0
            description 'sample description'     

            repositories { mavenCentral(); mavenLocal() }
            dependencies {        
                implementation platform('io.dropwizard:dropwizard-dependencies:2.0.0')
                constraints {
                    implementation 'junit:junit:4.11'
                }
    
                provided 'junit:junit' 
                implementation 'io.dropwizard:dropwizard-core'
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

        then: "dependencyManagement block exists"
        // note that in dependencyManagement scope for junit will be runtime.. doesn't matter
        pom.dependencyManagement.dependencies.dependency.size() == 2

        then: "only two dependencies"
        pom.dependencies.dependency.size() == 2

        then: "compile dependency scope correct"
        def dep = pom.dependencies.'*'.find { it.artifactId.text() == 'dropwizard-core' }
        dep.scope.text() == 'compile'
        !dep.version

        then: "provided dependency scope correct"
        def dep2 = pom.dependencies.'*'.find { it.artifactId.text() == 'junit' }
        dep2.scope.text() == 'provided'
        !dep2.version

    }
}
