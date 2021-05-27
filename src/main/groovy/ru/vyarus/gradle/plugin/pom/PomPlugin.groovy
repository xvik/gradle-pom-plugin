package ru.vyarus.gradle.plugin.pom

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.artifacts.configurations.ConfigurationContainerInternal
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import ru.vyarus.gradle.plugin.pom.xml.XmlMerger

/**
 * Pom plugin "fixes" maven-publish plugin pom generation: set correct scopes for dependencies.
 * <p>
 * Plugin must be applied after java, java-library, groovy plugin or java-platform (used for BOM).
 * <p>
 * Applies new configurations:
 * <ul>
 *      <li>provided</li>
 *      <li>optional</li>
 * </ul>
 * Implementation configuration extends from both, so build consider all dependencies types as compile.
 * The difference is visible only in the resulted pom. Note that compileOnly CAN'T be used instead of provided
 * because such dependencies are not available in tests. By the same reason gradle feature variants are
 * bad candidates for optionals.
 * <p>
 * During pom generation dependencies scope is automatically fixed for implementation (compile) dependencies
 * (gradle always set runtime for them), compileOnly dependencies are added to pom as provided.
 * <p>
 * Plugin implicitly activates maven-publish plugin. But publication still must be configured manually:
 * pom plugin only fixes behaviour, but not replace configuration.
 * <p>
 * Plugin adds simplified pom configuration extension. Using pom closure in build new sections could be added
 * to resulted pom. If multiple maven publications configured, pom modification will be applied to all of them.
 * <p>
 * NOTE: When used with java-platform no additional configurations applied and no automatic scopes modifications
 * applied to generated pom (user defined modifications (pom and withPomXml blocks) would work as expected).
 *
 * @see PomExtension use `pomGeneration` closure to configure pom generation behaviour
 * @author Vyacheslav Rusakov
 * @since 04.11.2015
 */
@CompileStatic(TypeCheckingMode.SKIP)
class PomPlugin implements Plugin<Project> {

    private static final String COMPILE = 'compile'
    private static final String RUNTIME = 'runtime'
    private static final String PROVIDED = 'provided'
    private static final String OPTIONAL = 'optional'

    @Override
    void apply(Project project) {
        // activate maven-publish automatically for java modules
        project.plugins.withType(JavaPlugin) {
            project.plugins.apply(MavenPublishPlugin)
        }
        // activate maven-publish automatically for java-platform modules (BOM declaration)
        project.plugins.withId('java-platform') {
            project.plugins.apply(MavenPublishPlugin)
        }
        // in case when java plugin is not used and maven plugin activated manually
        // (case: using java-platform for BOM publication)
        project.plugins.withType(MavenPublishPlugin) {
            // extensions mechanism not used because we need free closure for pom xml modification
            project.convention.plugins.pom = new PomConvention()
            // used to configure applied pom modifications
            project.extensions.create('pomGeneration', PomExtension)
            // additional configurations are not useful for BOM
            project.plugins.withType(JavaPlugin) {
                addConfigurations(project)
            }
            activatePomModifications(project)
        }
    }

    /**
     * Adds honest provided and optional implementations.
     *
     * @param project project
     */
    private void addConfigurations(Project project) {
        ConfigurationContainer configurations = project.configurations
        Configuration provided = configurations.create(PROVIDED)
        provided.description =
                'Provided works the same as implementation configuration and only affects resulted pom'

        Configuration optional = configurations.create(OPTIONAL)
        optional.description =
                'Optional works the same as implementation configuration and only affects resulted pom'

        configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(provided, optional)
    }

    private void activatePomModifications(Project project) {
        PublishingExtension publishing = project.publishing
        // apply to all configured maven publications (even not yet registered)
        publishing.publications.withType(MavenPublication) {
            // important to apply after possible user modifications because otherwise duplicate tags will arise
            project.afterEvaluate {
                PomExtension extension = project.extensions.findByType(PomExtension)
                if (extension.forcedVersions) {
                    // Recommended way: see https://docs.gradle.org/current/userguide/publishing_maven.html
                    // #publishing_maven:resolved_dependencies
                    versionMapping {
                        usage('java-api') {
                            fromResolutionOf('runtimeClasspath')
                        }
                        usage('java-runtime') {
                            fromResolutionResult()
                        }
                    }
                }
                pom.withXml {
                    Node pomXml = asNode()
                    // do nothing for BOM
                    project.plugins.withType(JavaPlugin) {
                        fixPomDependenciesScopes(project, pomXml)
                    }
                    if (extension.removedDependencyManagement) {
                        // remove dependenciesManagementSection
                        NodeList dependencyManagement = pomXml.dependencyManagement
                        if (!dependencyManagement.empty) {
                            pomXml.remove(dependencyManagement.first())
                        }
                    }
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
    @SuppressWarnings('MethodSize')
    private void fixPomDependenciesScopes(Project project, Node pomXml) {
        Node dependencies = pomXml.dependencies[0]
        Closure correctDependencies = { DependencySet deps, Closure action ->
            if (!dependencies || deps.empty) {
                // avoid redundant searches (if no deps in xml or no artifacts in configuration)
                // for example, this may arise if gradleApi() used as dependency
                return
            }
            dependencies.dependency.findAll {
                deps.find { Dependency dep ->
                    dep.group == it.groupId.text() && dep.name == it.artifactId.text()
                }
            }.each(action)
        }
        Closure correctScope = { DependencySet deps, String requiredScope ->
            correctDependencies(deps) { it.scope*.value = requiredScope }
        }

        ConfigurationContainer configurations = project.configurations
        Configuration implementation = configurations.implementation

        // corrections may be disabled but optional and provided configurations have to be always "corrected"
        boolean fixScopes = !project.extensions.findByType(PomExtension).disabledScopesCorrection

        if (fixScopes) {
            correctScope(implementation.allDependencies, COMPILE)
        }

        // OPTIONAL
        correctDependencies(
                configurations.optional.allDependencies - (implementation.dependencies) as DependencySet) {
            it.scope*.value = COMPILE
            it.appendNode(OPTIONAL, true.toString())
        }

        // PROVIDED
        correctScope(configurations.provided.allDependencies - (implementation.dependencies) as DependencySet,
                PROVIDED)

        if (fixScopes) {
            // deprecated configurations fixes: existence check is required as they will be removed in gradle 7 (or 8)
            if ((configurations as ConfigurationContainerInternal).findByName(RUNTIME) != null) {
                // not allDependencies because runtime extends compile
                correctScope(configurations.runtime.dependencies, RUNTIME)
            }
            if ((configurations as ConfigurationContainerInternal).findByName(COMPILE) != null) {
                correctScope(configurations.compile.allDependencies, COMPILE)
            }

            // war plugin configurations by default added as compile, which is wrong
            project.plugins.withType(WarPlugin) {
                correctScope(configurations.providedCompile.allDependencies, PROVIDED)
                correctScope(configurations.providedRuntime.allDependencies, PROVIDED)
            }
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
