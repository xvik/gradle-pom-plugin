package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2016
 */
class PomSectionsTest extends AbstractKitTest {

    def "Check pom properties section"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            group 'ru.vyarus'
            version 1.0

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pom {
                groupId 'test'
                artifactId 'name'
                version 1.0
                packaging 'pom'

                name 'long name'
                description 'shot description'
                url 'some url'
                inceptionYear 2012

                dependencies {
                    dependency {
                      groupId 'junit'
                      artifactId 'junit'
                      type 'jar'
                      scope 'test'
                      optional 'true'
                    }
                }

                'parent' {
                    groupId 'org.codehaus.mojo'
                    artifactId 'my-parent'
                    version 2.0
                    delegate.relativePath '../my-parent'
                }

                dependencyManagement {
                    dependency {
                      groupId 'junit'
                      artifactId 'junit'
                      version 4.0
                      type 'jar'
                      scope 'test'
                      optional 'true'
                    }
                }

                modules {
                    module 'my-project'
                    module 'another-project'
                }

                properties {
                    prop1 'val1'
                    prop2 'val2'
                }

                build {
                    directory "\${project.rootDir}"
                    outputDirectory "\${project.buildDir}/classes"
                    finalName "\${project.name}-\${project.version}"
                    testOutputDirectory "\${project.buildDir}/test-classes"
                    sourceDirectory "\${project.rootDir}/src/main/java"
                    scriptSourceDirectory "src/main/scripts"
                    testSourceDirectory "\${project.rootDir}/src/test/java"
                    resources {
                        resource {
                            directory "\${project.rootDir}/src/main/resources"
                        }
                    }
                    testResources {
                        testResource {
                            directory "\${project.rootDir}/src/test/resources"
                        }
                    }
                    pluginManagement {
                        plugins {
                            plugin {
                                artifactId 'maven-antrun-plugin'
                                version 1.3
                            }
                        }
                    }
                }

                reporting {
                     outputDirectory "\${project.buildDir}/site"
                }

                licenses {
                    license {
                        name 'Apache License, Version 2.0'
                        url 'https://www.apache.org/licenses/LICENSE-2.0.txt'
                        distribution 'repo'
                        comments 'A business-friendly OSS license'
                    }
                }

                organization {
                    name 'Codehaus Mojo'
                    url 'http://mojo.codehaus.org'
                }

                developers {
                    developer {
                        id 'jdoe'
                        name 'John Doe'
                        email 'jdoe@example.com'
                        url 'http://www.example.com/jdoe'
                        organization 'ACME'
                        organizationUrl 'http://www.example.com'
                        roles {
                            role 'architect'
                            role 'developer'
                        }
                        timezone 'America/New_York'
                        properties {
                            picUrl 'http://www.example.com/jdoe/pic'
                        }
                    }
                }

                contributors {
                    contributor {
                        name 'Noelle'
                        email 'some.name@gmail.com'
                        url 'http://noellemarie.com'
                        organization 'Noelle Marie'
                        organizationUrl 'http://noellemarie.com'
                        roles {
                            role 'tester'
                        }
                        timezone 'America/Vancouver'
                        properties {
                            gtalk 'some.name@gmail.com'
                        }
                    }
                }

                issueManagement {
                    system 'Bugzilla'
                    url 'http://127.0.0.1/bugzilla/'
                }

                ciManagement {
                    system 'continuum'
                    url 'http://127.0.0.1:8080/continuum'
                    notifiers {
                        notifier {
                            type 'mail'
                            sendOnError 'true'
                            sendOnFailure 'true'
                            sendOnSuccess 'false'
                            sendOnWarning 'false'
                            configuration {
                                address 'continuum@127.0.0.1'
                            }
                        }
                    }
                }

                mailingLists {
                    mailingList {
                        name 'User List'
                        subscribe 'user-subscribe@127.0.0.1'
                        unsubscribe 'user-unsubscribe@127.0.0.1'
                        post 'user@127.0.0.1'
                        archive 'http://127.0.0.1/user/'
                        otherArchives {
                            otherArchive 'http://base.google.com/base/1/127.0.0.1'
                        }
                    }
                }

                scm {
                    connection 'scm:svn:http://127.0.0.1/svn/my-project'
                    developerConnection 'scm:svn:https://127.0.0.1/svn/my-project'
                    tag 'HEAD'
                    url 'http://127.0.0.1/websvn/my-project'
                }

                prerequisites {
                    maven '2.0.6'
                }

                repositories {
                     repository {
                        id 'central'
                        name 'Central Repository'
                        url 'http://repo.maven.apache.org/maven2'
                        layout 'default'
                        snapshots {
                            enabled 'false'
                        }
                    }
                }

                pluginRepositories {
                    pluginRepository {
                        id 'central'
                        name 'Central Repository'
                        url 'http://repo.maven.apache.org/maven2'
                        layout 'default'
                        snapshots {
                            enabled 'false'
                        }
                        releases {
                            updatePolicy 'never'
                        }
                    }
                }

                distributionManagement {
                    downloadUrl 'http://mojo.codehaus.org/my-project'
                    status 'deployed'
                    repository {
                        uniqueVersion 'false'
                        id 'corp1'
                        name 'Corporate Repository'
                        url 'scp://repo/maven2'
                        layout 'default'
                    }
                    snapshotRepository {
                        uniqueVersion 'true'
                        id 'propSnap'
                        name 'Propellors Snapshots'
                        url 'sftp://propellers.net/maven'
                        layout 'legacy'
                    }
                    site {
                        id 'mojo.website'
                        name 'Mojo Website'
                        url 'scp://beaver.codehaus.org/home/projects/mojo/public_html/'
                    }
                }

                profiles {
                    profile {
                        id 'release-profile'
                        activation {
                            property {
                                name 'performRelease'
                                value 'true'
                            }
                        }
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
        // validates xml correctness
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "valid properties section"
        """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>name</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>
  <name>long name</name>
  <description>shot description</description>
  <url>some url</url>
  <inceptionYear>2012</inceptionYear>
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <type>jar</type>
      <scope>test</scope>
      <optional>true</optional>
    </dependency>
  </dependencies>
  <parent>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>my-parent</artifactId>
    <version>2.0</version>
    <relativePath>../my-parent</relativePath>
  </parent>
  <dependencyManagement>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.0</version>
      <type>jar</type>
      <scope>test</scope>
      <optional>true</optional>
    </dependency>
  </dependencyManagement>
  <modules>
    <module>my-project</module>
    <module>another-project</module>
  </modules>
  <properties>
    <prop1>val1</prop1>
    <prop2>val2</prop2>
  </properties>
  <build>
    <directory>/tmp/junit4041206865739771722</directory>
    <outputDirectory>/tmp/junit4041206865739771722/build/classes</outputDirectory>
    <finalName>junit4041206865739771722-1.0</finalName>
    <testOutputDirectory>/tmp/junit4041206865739771722/build/test-classes</testOutputDirectory>
    <sourceDirectory>/tmp/junit4041206865739771722/src/main/java</sourceDirectory>
    <scriptSourceDirectory>src/main/scripts</scriptSourceDirectory>
    <testSourceDirectory>/tmp/junit4041206865739771722/src/test/java</testSourceDirectory>
    <resources>
      <resource>
        <directory>/tmp/junit4041206865739771722/src/main/resources</directory>
      </resource>
    </resources>
    <testResources>
      <testResource>
        <directory>/tmp/junit4041206865739771722/src/test/resources</directory>
      </testResource>
    </testResources>
    <pluginManagement>
      <plugins>
        <plugin>
          <artifactId>maven-antrun-plugin</artifactId>
          <version>1.3</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
  <reporting>
    <outputDirectory>/tmp/junit4041206865739771722/build/site</outputDirectory>
  </reporting>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
      <comments>A business-friendly OSS license</comments>
    </license>
  </licenses>
  <organization>
    <name>Codehaus Mojo</name>
    <url>http://mojo.codehaus.org</url>
  </organization>
  <developers>
    <developer>
      <id>jdoe</id>
      <name>John Doe</name>
      <email>jdoe@example.com</email>
      <url>http://www.example.com/jdoe</url>
      <organization>ACME</organization>
      <organizationUrl>http://www.example.com</organizationUrl>
      <roles>
        <role>architect</role>
        <role>developer</role>
      </roles>
      <timezone>America/New_York</timezone>
      <properties>
        <picUrl>http://www.example.com/jdoe/pic</picUrl>
      </properties>
    </developer>
  </developers>
  <contributors>
    <contributor>
      <name>Noelle</name>
      <email>some.name@gmail.com</email>
      <url>http://noellemarie.com</url>
      <organization>Noelle Marie</organization>
      <organizationUrl>http://noellemarie.com</organizationUrl>
      <roles>
        <role>tester</role>
      </roles>
      <timezone>America/Vancouver</timezone>
      <properties>
        <gtalk>some.name@gmail.com</gtalk>
      </properties>
    </contributor>
  </contributors>
  <issueManagement>
    <system>Bugzilla</system>
    <url>http://127.0.0.1/bugzilla/</url>
  </issueManagement>
  <ciManagement>
    <system>continuum</system>
    <url>http://127.0.0.1:8080/continuum</url>
    <notifiers>
      <notifier>
        <type>mail</type>
        <sendOnError>true</sendOnError>
        <sendOnFailure>true</sendOnFailure>
        <sendOnSuccess>false</sendOnSuccess>
        <sendOnWarning>false</sendOnWarning>
        <configuration>
          <address>continuum@127.0.0.1</address>
        </configuration>
      </notifier>
    </notifiers>
  </ciManagement>
  <mailingLists>
    <mailingList>
      <name>User List</name>
      <subscribe>user-subscribe@127.0.0.1</subscribe>
      <unsubscribe>user-unsubscribe@127.0.0.1</unsubscribe>
      <post>user@127.0.0.1</post>
      <archive>http://127.0.0.1/user/</archive>
      <otherArchives>
        <otherArchive>http://base.google.com/base/1/127.0.0.1</otherArchive>
      </otherArchives>
    </mailingList>
  </mailingLists>
  <scm>
    <connection>scm:svn:http://127.0.0.1/svn/my-project</connection>
    <developerConnection>scm:svn:https://127.0.0.1/svn/my-project</developerConnection>
    <tag>HEAD</tag>
    <url>http://127.0.0.1/websvn/my-project</url>
  </scm>
  <prerequisites>
    <maven>2.0.6</maven>
  </prerequisites>
  <repositories>
    <repository>
      <id>central</id>
      <name>Central Repository</name>
      <url>http://repo.maven.apache.org/maven2</url>
      <layout>default</layout>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>central</id>
      <name>Central Repository</name>
      <url>http://repo.maven.apache.org/maven2</url>
      <layout>default</layout>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
      <releases>
        <updatePolicy>never</updatePolicy>
      </releases>
    </pluginRepository>
  </pluginRepositories>
  <distributionManagement>
    <downloadUrl>http://mojo.codehaus.org/my-project</downloadUrl>
    <status>deployed</status>
    <repository>
      <uniqueVersion>false</uniqueVersion>
      <id>corp1</id>
      <name>Corporate Repository</name>
      <url>scp://repo/maven2</url>
      <layout>default</layout>
    </repository>
    <snapshotRepository>
      <uniqueVersion>true</uniqueVersion>
      <id>propSnap</id>
      <name>Propellors Snapshots</name>
      <url>sftp://propellers.net/maven</url>
      <layout>legacy</layout>
    </snapshotRepository>
    <site>
      <id>mojo.website</id>
      <name>Mojo Website</name>
      <url>scp://beaver.codehaus.org/home/projects/mojo/public_html/</url>
    </site>
  </distributionManagement>
  <profiles>
    <profile>
      <id>release-profile</id>
      <activation>
        <property>
          <name>performRelease</name>
          <value>true</value>
        </property>
      </activation>
    </profile>
  </profiles>
</project>
""".replaceAll("\r", "")  == pomFile.getText().replaceAll(testProjectDir.getRoot().name,"junit4041206865739771722")

    }

}
