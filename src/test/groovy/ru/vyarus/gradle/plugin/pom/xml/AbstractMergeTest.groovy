package ru.vyarus.gradle.plugin.pom.xml

import groovy.xml.XmlUtil
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 28.07.2016
 */
abstract class AbstractMergeTest extends Specification {

    static Node merge(String xml, Closure update) {
        Node base = new XmlParser().parseText(xml)
        XmlMerger.mergePom(base, update)
        base
    }

    static String xml(Node base) {
        def res = "\n" + XmlUtil.serialize(base).replaceAll("\r", "").replaceAll(" +\n", "\n")
        println res
        res
    }
}
