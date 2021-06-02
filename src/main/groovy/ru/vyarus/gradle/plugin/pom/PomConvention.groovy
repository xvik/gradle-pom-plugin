package ru.vyarus.gradle.plugin.pom

import groovy.transform.CompileStatic

/**
 * {@link PomPlugin} extension container. Not used as extension (project.extensions), but as convention
 * (project.conventions) because actual closure is structure free may define any pom sections (free xml).
 * <p>
 * Example usage:
 * <pre>
 * <code>
 *     pom {
 *         licenses {
 *              license {
 *                  name "The MIT License"
 *              }
 *         }
 *         developers {
 *             developer {
 *                 id "dev1"
 *                 name "Dev1 Name"
 *                 email "dev1@email.com"
 *             }
 *         }
 *     }
 * </code>
 * <pre>
 * Multiple declarations could be used.
 * <p>
 * If manual pom modification is required use:
 * <pre><code>
 *     withPomXml {
 *         it.appendNode('description', 'A demonstration of maven POM customization')
 *     }
 * </code></pre>
 * withPomXml convention usage is equivalent to maven-publish plugin withXml closure, but without need to call
 * asNode() because node is already provided as parameter.
 *
 * @author Vyacheslav Rusakov
 * @since 04.11.2015
 */
@CompileStatic
class PomConvention {
    List<Closure> configs = []
    List<Closure> xmlModifiers = []

    /**
     * @param config user pom
     */
    void pom(Closure config) {
        this.configs.add(config)
    }

    /**
     * Modification closure is called just after user pom merge. Pom xml passed to closure as {@link Node} parameter.
     * @param modifier manual pom modification closure
     */
    void withPomXml(Closure modifier) {
        this.xmlModifiers.add(modifier)
    }
}
