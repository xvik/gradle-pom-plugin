package ru.vyarus.gradle.plugin.pom

import groovy.xml.XmlParser
import org.gradle.testkit.runner.BuildResult

/**
 * @author Vyacheslav Rusakov
 * @since 14.02.2024
 */
class DebugKitTest extends AbstractKitTest {

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
                provided 'com.google.code.findbugs:annotations:3.0.0'
                // runtime
                runtimeOnly 'ru.vyarus:guice-ext-annotations:1.1.1'  
                optional 'ru.vyarus:generics-resolver:2.0.0'
                // disappear    
                compileOnly 'junit:junit:4.12'
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
        debug()
        BuildResult result = run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "implementation dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'

        then: "provided dependency scope corrected"
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'

        then: "runtimeOnly dependency scope correct"
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice-ext-annotations' }.scope.text() == 'runtime'

        then: "optional dependency scope corrected"
        def opt = pom.dependencies.'*'.find { it.artifactId.text() == 'generics-resolver' }
        opt.scope.text() == 'compile'
        opt.optional.text() == 'true'

        then: "compileOnly dependencies are removed from pom"
        pom.dependencies.'*'.find { it.artifactId.text() == 'junit' } == null

        then: "pom modification applied"
        def developer = pom.developers.developer
        developer.id.text() == 'dev'
        developer.name.text() == 'Dev Dev'
        developer.email.text() == 'dev@gmail.com'
        pom.scm.url.text() == 'http://sdsds.dd'
        pom.inceptionYear.text() == '2020'

        then: "defaults applied"
        pom.name.text() != null
        !pom.name.text().empty
        pom.description.text() == 'sample description'

        then: 'debug logs correct'
        unifyDebugString(result.output).contains("""> Configure project :
POM> Apply 1 pom model customizations for maven publication

POM> --------------------------------- Applied XML model changes for maven publication
\t  10 |   <artifactId>junit4041206865739771722</artifactId>
\t  11 |   <version>1.0</version>
\t  12 | +  <developers>+
\t  13 | +    <developer>+
\t  14 | +      <id>dev</id>+
\t  15 | +      <name>Dev Dev</name>+
\t  16 | +      <email>dev@gmail.com</email>+
\t  17 | +    </developer>+
\t  18 | +  </developers>+
     --------------------------------

> Task :generatePomFileForMavenPublication
POM> Correct compile dependencies for maven publication
\t - org.javassist:javassist:3.16.1-GA (original scope: runtime)
\t - com.google.code.findbugs:annotations:3.0.0 (original scope: runtime)
\t - ru.vyarus:generics-resolver:2.0.0 (original scope: runtime)

POM> Correct optional dependencies for maven publication
\t - ru.vyarus:generics-resolver:2.0.0 (original scope: compile)

POM> Correct provided dependencies for maven publication
\t - com.google.code.findbugs:annotations:3.0.0 (original scope: compile)

POM> Apply 1 withXml closures for maven publication
POM> Apply 1 withPomXml customizations for maven publication
POM> Apply default name for maven publication
POM> Apply default description for maven publication

POM> --------------------------------- Applied direct XML changes for maven publication
\t  16 |       <artifactId>javassist</artifactId>
\t  17 |       <version>3.16.1-GA</version>
\t  18 |       <scope>-runtime-+compile+</scope>
\t
\t  22 |       <artifactId>annotations</artifactId>
\t  23 |       <version>3.0.0</version>
\t  24 |       <scope>-runtime-+provided+</scope>
\t
\t  28 |       <artifactId>generics-resolver</artifactId>
\t  29 |       <version>2.0.0</version>
\t  30 |       <scope>-runtime-+compile+</scope>
\t  31 | +      <optional>true</optional>+
\t
\t  38 |     </dependency>
\t  39 |   </dependencies>
\t  40 | +  <scm>+
\t  41 | +    <url>http://sdsds.dd</url>+
\t  42 | +  </scm>+
\t  43 | +  <inceptionYear>2020</inceptionYear>+
\t  44 | +  <name>junit4041206865739771722</name>+
\t  45 | +  <description>sample description</description>+
     --------------------------------
""")
    }

    def "Check multiple modification blocks debug"() {
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
                
                pom {
                    developers {
                        developer {
                            id = "dev2"
                            name = "Dev2 Dev2"
                            email = "dev2@gmail.com"
                        }
                    }
                }   


                withPom {
                    scm {
                        url 'http://sdsds.dd'
                    }
                }                        

                withPom {
                    scm {
                        connection 'http://sdsds.dd'
                    }
                }                            

                withPomXml {
                    asNode().appendNode('inceptionYear', 2020)
                }

                withPomXml {
                    asNode().appendNode('supportYear', 2022)
                }
            }

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run pom task"
        debug()
        BuildResult result = run('generatePomFileForMavenPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "debug output correct"
        unifyDebugString(result.output).contains("""> Configure project :
POM> Apply 2 pom model customizations for maven publication

POM> --------------------------------- Applied XML model changes for maven publication
\t  10 |   <artifactId>junit4041206865739771722</artifactId>
\t  11 |   <version>1.0</version>
\t  12 | +  <developers>+
\t  13 | +    <developer>+
\t  14 | +      <id>dev</id>+
\t  15 | +      <name>Dev Dev</name>+
\t  16 | +      <email>dev@gmail.com</email>+
\t  17 | +    </developer>+
\t  18 | +    <developer>+
\t  19 | +      <id>dev2</id>+
\t  20 | +      <name>Dev2 Dev2</name>+
\t  21 | +      <email>dev2@gmail.com</email>+
\t  22 | +    </developer>+
\t  23 | +  </developers>+
     --------------------------------

> Task :generatePomFileForMavenPublication
POM> Apply 2 withXml closures for maven publication
POM> Apply 2 withPomXml customizations for maven publication
POM> Apply default name for maven publication
POM> Apply default description for maven publication

POM> --------------------------------- Applied direct XML changes for maven publication
\t  16 |     </developer>
\t  17 |   </developers>
\t  18 | +  <scm>+
\t  19 | +    <url>http://sdsds.dd</url>+
\t  20 | +    <connection>http://sdsds.dd</connection>+
\t  21 | +  </scm>+
\t  22 | +  <inceptionYear>2020</inceptionYear>+
\t  23 | +  <supportYear>2022</supportYear>+
\t  24 | +  <name>junit4041206865739771722</name>+
\t  25 | +  <description>sample description</description>+
     --------------------------------
""")
    }


    def "Check boms reorder debug"() {
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

            maven.debug()

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
        debug()
        BuildResult result = run('generatePomFileForBOMPublication')

        def pomFile = file("build/generated-pom.xml")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "debug output correct"
        unifyDebugString(result.output).contains("""> Task :generatePomFileForBOMPublication
POM> Move org.pf4j:pf4j import at the top for BOM publication
POM> Move org.pf4j:pf4j-update import at the top for BOM publication
POM> Apply default name for BOM publication
POM> Apply default description for BOM publication

POM> --------------------------------- Applied direct XML changes for BOM publication
\t   8 |     <dependencies>
\t   9 |       <dependency>
\t  10 | +        <groupId>ru.vyarus.guicey</groupId>+
\t  11 | +        <artifactId>guicey-bom</artifactId>+
\t  12 | +        <version>5.2.0-1</version>+
\t  13 | +        <type>pom</type>+
\t  14 | +        <scope>import</scope>+
\t  15 | +      </dependency>+
\t  16 | +      <dependency>+
\t
\t  24 |         <version>2.3.0</version>
\t  25 |       </dependency>
\t  26 | -      <dependency>-
\t  27 | -        <groupId>ru.vyarus.guicey</groupId>-
\t  28 | -        <artifactId>guicey-bom</artifactId>-
\t  29 | -        <version>5.2.0-1</version>-
\t  30 | -        <type>pom</type>-
\t  31 | -        <scope>import</scope>-
\t  32 | -      </dependency>-
\t  33 |     </dependencies>
\t  34 |   </dependencyManagement>
\t  35 | +  <name>junit4041206865739771722</name>+
\t  36 | +  <description>sample description</description>+
     --------------------------------
""")
    }


    private String unifyDebugString(String text) {
        unifyString(text)
                .replace(testProjectDir.name, "junit4041206865739771722")
                .replaceAll("\u001B\\[[;\\d]*m", "")
    }
}
