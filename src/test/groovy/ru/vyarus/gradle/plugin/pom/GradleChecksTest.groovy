package ru.vyarus.gradle.plugin.pom

/**
 * @author Vyacheslav Rusakov
 * @since 04.07.2018
 */
class GradleChecksTest extends AbstractKitTest {

    def "Check fail on gradle below 4.6"() {
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

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run on older gradle"
        def res = runFailedVer('4.5', 'generatePomFileForMavenPublication')

        then: "failed"
        res.output.contains("java.lang.ClassNotFoundException: org.gradle.api.internal.FeaturePreviews")
    }

    def "Check manually enabled strict publishing"() {

        setup:
        file('settings.gradle') << 'enableFeaturePreview(\'STABLE_PUBLISHING\')'
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

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run"
        def res = run('generatePomFileForMavenPublication')

        then: "no warning"
        !res.output.contains("STABLE_PUBLISHING preview option enabled by")

    }

    def "Check strict publish automatic enabling"() {

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

            model {
                tasks.generatePomFileForMavenPublication {
                    destination = file("\$buildDir/generated-pom.xml")
                }
            }
        """)

        when: "run"
        def res = run('generatePomFileForMavenPublication')

        then: "no warning"
        res.output.contains("STABLE_PUBLISHING preview option enabled by")
    }
}
