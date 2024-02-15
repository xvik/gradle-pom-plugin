package ru.vyarus.gradle.plugin.pom.xml

/**
 * @author Vyacheslav Rusakov
 * @since 28.07.2016
 */
class MultipleTagsRenderTest extends AbstractMergeTest {

    def "Check multiple tags insert"() {

        setup:
        Node base = merge("<project></project>") {
            developers {
                developer {
                    id "dev1"
                    name "Dev1"
                    email "dev@dev1.com"
                }
                developer {
                    id "dev2"
                    name "Dev2"
                    email "dev2@dev2.com"
                }
            }
        }

        expect:
        xml(base) == """
<?xml version="1.0" encoding="UTF-8"?><project>
  <developers>
    <developer>
      <id>dev1</id>
      <name>Dev1</name>
      <email>dev@dev1.com</email>
    </developer>
    <developer>
      <id>dev2</id>
      <name>Dev2</name>
      <email>dev2@dev2.com</email>
    </developer>
  </developers>
</project>
"""
    }

    def "Check multiple tags merge"() {

        setup:
        Node base = merge("""
    <project>
        <developers>
            <developer>
                <id>test</id>
            </developer>
        </developers>
    </project>
    """) {
            developers {
                developer {
                    id "dev1"
                    name "Dev1"
                    email "dev@dev1.com"
                }
                developer {
                    id "dev2"
                    name "Dev2"
                    email "dev2@dev2.com"
                }
            }
        }

        expect:
        xml(base) == """
<?xml version="1.0" encoding="UTF-8"?><project>
  <developers>
    <developer>
      <id>dev1</id>
      <name>Dev1</name>
      <email>dev@dev1.com</email>
    </developer>
    <developer>
      <id>dev2</id>
      <name>Dev2</name>
      <email>dev2@dev2.com</email>
    </developer>
  </developers>
</project>
"""
    }

    def "Check multiple tags complex merge"() {

        setup:
        Node base = merge("""
    <project>
        <developers>
            <developer>
              <id>dev3</id>
            </developer>
            <developer>
              <id>dev4</id>
            </developer>
        </developers>
    </project>
    """) {
            developers {
                developer {
                    id "dev1"
                    name "Dev1"
                    email "dev@dev1.com"
                }
                developer {
                    id "dev2"
                    name "Dev2"
                    email "dev2@dev2.com"
                }
            }
        }

        expect:
        xml(base) == """
<?xml version="1.0" encoding="UTF-8"?><project>
  <developers>
    <developer>
      <id>dev1</id>
      <name>Dev1</name>
      <email>dev@dev1.com</email>
    </developer>
    <developer>
      <id>dev2</id>
      <name>Dev2</name>
      <email>dev2@dev2.com</email>
    </developer>
  </developers>
</project>
"""
    }

    def "Check multiple tags complex merge 2"() {

        setup:
        Node base = merge("""
    <project>
        <developers>
            <developer>
              <id>dev3</id>
            </developer>
            <developer>
              <id>dev4</id>
            </developer>
            <developer>
              <id>dev5</id>
            </developer>
        </developers>
    </project>
    """) {
            developers {
                developer {
                    id "dev1"
                    name "Dev1"
                    email "dev@dev1.com"
                }
                developer {
                    id "dev2"
                    name "Dev2"
                    email "dev2@dev2.com"
                }
            }
        }

        expect:
        xml(base) == """
<?xml version="1.0" encoding="UTF-8"?><project>
  <developers>
    <developer>
      <id>dev1</id>
      <name>Dev1</name>
      <email>dev@dev1.com</email>
    </developer>
    <developer>
      <id>dev2</id>
      <name>Dev2</name>
      <email>dev2@dev2.com</email>
    </developer>
    <developer>
      <id>dev5</id>
    </developer>
  </developers>
</project>
"""
    }
}