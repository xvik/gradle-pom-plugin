package ru.vyarus.gradle.plugin.pom.xml

import groovy.xml.XmlParser
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 15.02.2024
 */
class XmlUtilsTest extends Specification {

    def "Check node to string"() {

        when: "to string node"
        String xml = """<?xml version="1.0" encoding="UTF-8"?><root>
  <some>value</some>
</root>"""
        then: "correct"
        XmlUtils.toString(node(xml)).trim() == xml
    }

    def "Check diff"() {

        expect: "diff valid"
        XmlUtils.diff("""<root>
    <old>dd</old>
    <some>value</some>
</root>
""", """<root>
    <some>value2</some>
    <other>vv</other>
</root>
""").replaceAll("\u001B\\[[;\\d]*m", "")  == """   0 | <root>
   1 |     -<old>dd<-+<some>value2<+/-old>-+some>+
   2 |     -<some>value<-+<other>vv<+/-some>-+other>+
"""
    }

    def "Check shifted diff"() {

        expect: "diff valid"
        XmlUtils.diffShifted("""<root>
    <old>dd</old>
    <some>value</some>
</root>
""", """<root>
    <some>value2</some>
    <other>vv</other>
</root>
""", '\t').replaceAll("\u001B\\[[;\\d]*m", "")  == """\t   0 | <root>
\t   1 |     -<old>dd<-+<some>value2<+/-old>-+some>+
\t   2 |     -<some>value<-+<other>vv<+/-some>-+other>+"""
    }

    private Node node(String xml) {
        new XmlParser().parseText(xml)
    }
}
