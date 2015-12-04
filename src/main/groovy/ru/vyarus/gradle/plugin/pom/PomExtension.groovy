package ru.vyarus.gradle.plugin.pom

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
 * Only one pom configuration may be defined: if multiple pom configurations defined, only the last one will be
 * applied
 *
 * @author Vyacheslav Rusakov
 * @since 04.11.2015
 */
class PomExtension {
    Closure config

    void pom(Closure config) {
        this.config = config
    }
}
