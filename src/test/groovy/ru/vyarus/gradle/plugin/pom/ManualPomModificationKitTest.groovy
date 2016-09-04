package ru.vyarus.gradle.plugin.pom
/**
 * @author Vyacheslav Rusakov
 * @since 04.09.2016
 */
class ManualPomModificationKitTest extends AbstractKitTest {

    def "Check pom manual modification"() {
        setup:
        build("""
            plugins {
                id 'java'
                id 'ru.vyarus.pom'
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            pom {
                name "override"
            }

            withPomXml {
                it.appendNode('tata', 'blabla')
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

        then: "all pom customizations applied"
        pom.name.text() == 'override'
        pom.tata.text() == 'blabla'
    }

}