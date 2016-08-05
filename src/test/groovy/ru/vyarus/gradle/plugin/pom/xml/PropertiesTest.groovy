package ru.vyarus.gradle.plugin.pom.xml

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2016
 */
class PropertiesTest  extends AbstractMergeTest {

    def "Check properties"() {

        setup:
        Node base = merge("<project></project>") {
            properties {
                name 'value'
            }
        }

        expect:
        xml(base) == """
<?xml version="1.0" encoding="UTF-8"?><project>
  <properties>
    <name>value</name>
  </properties>
</project>
"""
    }
}
