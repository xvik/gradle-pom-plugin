package ru.vyarus.gradle.plugin.pom

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.util.GradleVersion
import ru.vyarus.gradle.plugin.pom.xml.ConfigurationsModel
import ru.vyarus.gradle.plugin.pom.xml.XmlActions
import ru.vyarus.gradle.plugin.pom.xml.XmlUtils

import static ru.vyarus.gradle.plugin.pom.xml.XmlActions.debug
import static ru.vyarus.gradle.plugin.pom.xml.XmlActions.printDiff

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
@SuppressWarnings('Println')
class PomPlugin implements Plugin<Project> {

    public static final String PROVIDED = 'provided'
    public static final String OPTIONAL = 'optional'

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
            project.extensions.create('maven', PomExtension)

            // additional configurations are not useful for BOM
            project.plugins.withType(JavaPlugin) {
                addConfigurations(project)
            }

            project.afterEvaluate {
                activatePomModifications(project)
            }
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
        PublishingExtension publishing = project.extensions.getByType(PublishingExtension)
        String projectName = project.name
        // id used for debug only (to differentiate messages for different modules)
        String projectId = project.rootProject == project ? null : project.name
        String projectDesc = project.description
        // use RAW extension object so it could be serialized
        PomExtension extension = project.extensions.getByType(PomExtension).copy()
        Provider<ConfigurationsModel> configurations = project
                .provider { ConfigurationsModel.build(project.configurations) }
        // apply to all configured maven publications (even not yet registered)
        publishing.publications.withType(MavenPublication) { MavenPublication pub ->
            forceDependenciesVersions(pub, extension)
            applyPomModelModifiers(pub, extension, projectId)

            String pubName = pub.name
            boolean isJavaPlugin = project.plugins.hasPlugin(JavaPlugin)

            pom.withXml {
                Node pomXml = asNode()
                String before
                if (extension.debug) {
                    before = XmlUtils.toString(pomXml)
                }
                // do nothing for BOM
                if (isJavaPlugin) {
                    XmlActions.fixPomDependenciesScopes(projectId, extension, pubName, pomXml, configurations.get())
                }

                XmlActions.fixDependencyManagement(projectId, extension, pubName, pomXml)
                XmlActions.applyPomXmlModifiers(projectName, projectDesc, projectId, extension, pubName, it)
                if (extension.forcedVersions) {
                    XmlActions.validateVersions(pomXml)
                }

                if (extension.debug) {
                    String after = XmlUtils.toString(pomXml)
                    printDiff(projectId, extension.debug, pubName,
                            'Applied direct XML changes', before, after)
                }
            }
        }
    }

    private void forceDependenciesVersions(MavenPublication pub, PomExtension extension) {
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
    }

    private void applyPomModelModifiers(MavenPublication pub, PomExtension extension, String projectName) {
        String pubName = pub.name
        if (!extension.configs.empty) {
            pub.pom {
                String before
                if (extension.debug) {
                    // before 8.4 there were no static method for xml generation from model
                    if (GradleVersion.current() >= GradleVersion.version('8.4')) {
                        before = XmlUtils.toString(it)
                    }
                    debug(projectName, extension.debug, pubName,
                            "Apply ${extension.configs.size()} pom model customizations")
                }
                extension.configs.each { a -> a.execute(it) }
                if (extension.debug) {
                    if (before) {
                        String after = XmlUtils.toString(it)
                        printDiff(projectName, extension.debug, pubName, 'Applied XML model changes', before, after)
                    } else {
                        println '\tPom model diff is only supported for gradle 8.4 and above'
                    }
                }
            }
        }
    }
}
