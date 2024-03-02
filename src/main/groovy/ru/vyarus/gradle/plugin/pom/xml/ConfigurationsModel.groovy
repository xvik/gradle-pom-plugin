package ru.vyarus.gradle.plugin.pom.xml

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencySet

/**
 * In order to be compatible with configuration cache, all required dependencies must be resolved before xml
 * processing.
 *
 * @author Vyacheslav Rusakov
 * @since 28.02.2024
 */
@CompileStatic
class ConfigurationsModel {

    private static final String PROVIDED_COMPILE = 'providedCompile'
    private static final String IMPLEMENTATION = 'implementation'
    private static final String OPTIONAL = 'optional'
    private static final String PROVIDED = 'provided'
    private static final String PROVIDED_RUNTIME = 'providedRuntime'

    List<Dep> implementation
    List<Dep> optional
    List<Dep> provided
    List<Dep> providedCompile
    List<Dep> providedRuntime

    static ConfigurationsModel build(ConfigurationContainer configurations) {
        Configuration implementation = configurations.findByName(IMPLEMENTATION)
        boolean warPlugin = configurations.findByName(PROVIDED_COMPILE)

        return new ConfigurationsModel(
                implementation: convert(implementation.allDependencies),
                optional: convert(configurations.findByName(OPTIONAL).allDependencies -
                        implementation.dependencies as DependencySet),
                provided: convert(configurations.findByName(PROVIDED).allDependencies -
                        implementation.dependencies as DependencySet),
                providedCompile: warPlugin
                        ? convert(configurations.findByName(PROVIDED_COMPILE).allDependencies) : null,
                providedRuntime: warPlugin
                        ? convert(configurations.findByName(PROVIDED_RUNTIME).allDependencies) : null
        )
    }

    private static List<Dep> convert(DependencySet deps) {
        return deps.collect { new Dep(group: it.group, name: it.name, version: it.version) }
    }

    static class Dep {
        String group
        String name
        String version
    }
}
