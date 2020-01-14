package ru.vyarus.gradle.plugin.pom

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.artifacts.configurations.ConfigurationContainerInternal
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import ru.vyarus.gradle.plugin.pom.xml.XmlMerger

/**
 * Pom plugin "fixes" maven-publish plugin pom generation: set correct scopes for dependencies.
 * <p>
 * Plugin must be applied after java, java-library or groovy plugin.
 * <p>
 * During pom generation dependencies scope is automatically fixed for implementation (compile) dependencies
 * (gradle always set runtime for them), compileOnly dependencies are added to pom as provided.
 * <p>
 * Plugin implicitly activates maven-publish plugin. But publication still must be configured manually:
 * pom plugin only fixes behaviour, but not replace configuration.
 * <p>
 * Plugin adds simplified pom configuration extension. Using pom closure in build new sections could be added
 * to resulted pom. If multiple maven publications configured, pom modification will be applied to all of them.
 *
 * @author Vyacheslav Rusakov
 * @since 04.11.2015
 */
@CompileStatic(TypeCheckingMode.SKIP)
class PomPlugin implements Plugin<Project> {

    private static final String COMPILE = 'compile'
    private static final String RUNTIME = 'runtime'
    private static final String PROVIDED = 'provided'

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            // extensions mechanism not used because we need free closure for pom xml modification
            project.convention.plugins.pom = new PomConvention()

            project.plugins.apply(MavenPublishPlugin)
            activatePomModifications(project)
        }
    }

    private void activatePomModifications(Project project) {
        PublishingExtension publishing = project.publishing
        // apply to all configured maven publications (even not yet registered)
        publishing.publications.withType(MavenPublication) {
            // important to apply after possible user modifications because otherwise duplicate tags will arise
            project.afterEvaluate {
                pom.withXml {
                    Node pomXml = asNode()
                    fixPomDependenciesScopes(project, pomXml)
                    applyUserPom(project, pomXml)
                }
            }
        }
    }

    /**
     * Gradle sets runtime scope for all dependencies and this has to be fixed.
     *
     * @param project project instance
     * @param pomXml pom xml
     */
    private void fixPomDependenciesScopes(Project project, Node pomXml) {
        Node dependencies = pomXml.dependencies[0]
        Closure correctDependencies = { DependencySet deps, String requiredScope ->
            if (deps.empty) {
                return
            }
            dependencies.dependency.findAll {
                deps.find { Dependency dep ->
                    dep.group == it.groupId.text() && dep.name == it.artifactId.text()
                }
            }.each {
                it.scope*.value = requiredScope
            }
        }

        ConfigurationContainer configurations = project.configurations
        correctDependencies(configurations.implementation.allDependencies, COMPILE)
        // add "provided" dependencies
        addCompileOnlyDependencies(configurations, pomXml)

        // NOTE java-library's "api" configuration will be correctly added with compile scope

        // deprecated configurations fixes: existence check is required as they will be removed in gradle 7 (or 8)
        if ((configurations as ConfigurationContainerInternal).findByName(RUNTIME) != null) {
            // not allDependencies because runtime extends compile
            correctDependencies(configurations.runtime.dependencies, RUNTIME)
        }
        if ((configurations as ConfigurationContainerInternal).findByName(COMPILE) != null) {
            correctDependencies(configurations.compile.allDependencies, COMPILE)
        }
    }

    private void addCompileOnlyDependencies(ConfigurationContainer configurations, Node pomXml) {
        Node dependencies = pomXml.dependencies[0]
        // add compileOnly dependencies (not added by gradle)
        boolean hasXmlDeps = dependencies != null
        configurations.compileOnly.allDependencies.each {
            // check for duplicate declaration (just in case of incorrect declaration)
            if (hasXmlDeps && dependencies.dependency.find { dep ->
                dep.groupId.text() == it.group && dep.artifactId.text() == it.name
            }) {
                return
            }

            // could be null if no other dependencies declared
            Node deps = hasXmlDeps ? dependencies : pomXml.appendNode('dependencies')

            Node dep = deps.appendNode('dependency')
            dep.appendNode('groupId', it.group)
            dep.appendNode('artifactId', it.name)
            dep.appendNode('version', it.version)
            dep.appendNode('scope', PROVIDED)
        }
    }

    private void applyUserPom(Project project, Node pomXml) {
        PomConvention pomExt = project.convention.plugins.pom
        if (pomExt.config) {
            XmlMerger.mergePom(pomXml, pomExt.config)
        }
        pomExt.xmlModifier?.call(pomXml)
        // apply defaults if required
        if (!pomXml.name) {
            pomXml.appendNode('name', project.name)
        }
        if (project.description && !pomXml.description) {
            pomXml.appendNode('description', project.description)
        }
    }
}
