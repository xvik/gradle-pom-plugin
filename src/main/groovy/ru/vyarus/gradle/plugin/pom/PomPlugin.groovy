package ru.vyarus.gradle.plugin.pom

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import ru.vyarus.gradle.plugin.pom.xml.XmlMerger

/**
 * Pom plugin "fixes" maven-publish plugin pom generation: set correct scopes for dependencies. Also provides
 * global pom configuration (same as in maven publication, but applied to all publications).
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
 * Plugin provides several pom configurations:
 * <ul>
 *     <li>maven.pom - exactly the same as "publication".pom, but applied to all registered publications
 *     <li>maven.withPom - raw closure without structure (old way)
 *     <li>maven.withPomXml - same as "publication".pom.withXml, but applied to all registered publications
 * </ul>
 * <p>
 * Execution order:
 * <ul>
 *     <li>"publication" pom
 *     <li>plugin pom
 *     <li>publication withXml
 *     <li>plugin withPom
 *     <li>plugin withPomXml
 * </ul>
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
            project.extensions.create('maven', PomExtension, project)
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
        publishing.publications.withType(MavenPublication) { pub ->
            // important to apply after possible user modifications because otherwise duplicate tags will arise
            project.afterEvaluate {
                PomExtension extension = project.extensions.findByType(PomExtension)
                if (extension.forcedVersions) {
                    // Recommended way: see https://docs.gradle.org/current/userguide/publishing_maven.html
                    // #publishing_maven:resolved_dependencies
                    pub.versionMapping {
                        usage('java-api') {
                            fromResolutionOf('runtimeClasspath')
                        }
                        usage('java-runtime') {
                            fromResolutionResult()
                        }
                    }
                }
                extension.configs.forEach { pom(it) }
                pom.withXml {
                    Node pomXml = asNode()
                    // do nothing for BOM
                    project.plugins.withType(JavaPlugin) {
                        fixPomDependenciesScopes(project, extension, pomXml)
                    }
                    fixDependencyManagement(extension, pomXml)
                    applyPomModifiers(project, extension, it)
                    if (extension.forcedVersions) {
                        validateVersions(pomXml)
                    }
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
    private void fixPomDependenciesScopes(Project project, PomExtension extension, Node pomXml) {
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
        boolean fixScopes = !extension.disabledScopesCorrection

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
            // war plugin configurations by default added as compile, which is wrong
            project.plugins.withType(WarPlugin) {
                correctScope(configurations.providedCompile.allDependencies, PROVIDED)
                correctScope(configurations.providedRuntime.allDependencies, PROVIDED)
            }
        }
    }

    private void fixDependencyManagement(PomExtension extension, Node pomXml) {
        Node dependencyManagement = pomXml.dependencyManagement[0]
        if (!dependencyManagement) {
            return
        }
        if (extension.removedDependencyManagement) {
            // remove dependenciesManagementSection
            pomXml.remove(dependencyManagement)
        } else if (!extension.disabledBomsReorder) {
            // reorder BOMs (bubble BOMs to top)
            Node dependencies = dependencyManagement.dependencies[0]
            dependencies.dependency.findAll { it.scope.text() != 'import' }.each {
                dependencies.remove(it)
                dependencies.append(it)
            }
        }
    }

    private void applyPomModifiers(Project project, PomExtension pomExt, XmlProvider pomProvider) {
        Node pom = pomProvider.asNode()
        pomExt.rawConfigs.each {
            XmlMerger.mergePom(pom, it)
        }

        pomExt.xmlModifiers.each { it.execute(pomProvider) }
        // apply defaults if required
        if (!pom.name) {
            pom.appendNode('name', project.name)
        }
        if (project.description && !pom.description) {
            pom.appendNode('description', project.description)
        }
    }

    private void validateVersions(Node pomXml) {
        Node deps = pomXml.dependencies[0]
        if (!deps) {
            return
        }

        List<String> errors = []
        deps.dependency.findAll {
            if (!it.version.text()) {
                errors.add("\t${it.groupId.text()}:${it.artifactId.text()}\n")
            }
        }
        if (errors) {
            throw new GradleException('No versions resolved for the following dependencies. Most likely, there are ' +
                    'no required repositories declared. Declare missed repositories with, for example: ' +
                    'repositories { mavenCentral() }\n' + errors.join(''))
        }
    }
}
