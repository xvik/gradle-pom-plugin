package ru.vyarus.gradle.plugin.pom

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Pom plugin "fixes" maven-publish plugin pom generation.
 * <p>
 * Plugin must be applied after java or groovy plugin.
 * <p>
 * Applies new configurations:
 * <ul>
 *      <li>provided</li>
 *      <li>optional</li>
 * </ul>
 * Compile configuration extends from both, so build consider all dependencies types as compile.
 * The difference is visible only in resulted pom.
 * <p>
 * During pom generation dependencies scope is automatically fixed for compile dependencies (gradle always
 * set runtime for them) and provided and optional dependencies are also correctly marked.
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
class PomPlugin implements Plugin<Project> {

    private static final String OPTIONAL = 'optional'
    private static final String PROVIDED = 'provided'

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            // extensions mechanism not used because we need free closure for pom xml modification
            project.convention.plugins.pom = new PomExtension()

            project.plugins.apply(MavenPublishPlugin)

            addConfigurations(project)
            activatePomModifications(project)
        }
    }

    private void addConfigurations(Project project) {
        ConfigurationContainer configurations = project.configurations
        Configuration provided = configurations.create(PROVIDED)
        provided.setDescription('Provided works the same as compile configuration and only affects resulted pom')

        Configuration optional = configurations.create(OPTIONAL)
        optional.setDescription('Optional works the same as compile configuration and only affects resulted pom')

        configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(provided, optional)
    }

    private void activatePomModifications(Project project) {
        project.afterEvaluate {
            PublishingExtension publishing = project.publishing
            // apply to all configured maven publications
            publishing.publications.withType(MavenPublication) {
                pom.withXml {
                    Node pomXml = asNode()
                    fixPomDependenciesScopes(project, pomXml)
                    applyUserPom(project, pomXml)
                }
            }
        }
    }

    private void fixPomDependenciesScopes(Project project, Node pomXml) {
        // gradle sets runtime scope for all dependencies.. fixing
        pomXml.dependencies.dependency.findAll {
            it.scope.text() == JavaPlugin.RUNTIME_CONFIGURATION_NAME &&
                    project.configurations.compile.allDependencies.find { dep ->
                        dep.name == it.artifactId.text()
                    }
        }.each {
            it.scope*.value = JavaPlugin.COMPILE_CONFIGURATION_NAME
        }

        pomXml.dependencies.dependency.findAll {
            project.configurations.optional.allDependencies.find { dep ->
                dep.name == it.artifactId.text()
            }
        }.each {
            it.scope*.value = JavaPlugin.COMPILE_CONFIGURATION_NAME
            it.appendNode(OPTIONAL, 'true')
        }

        pomXml.dependencies.dependency.findAll {
            project.configurations.provided.allDependencies.find { dep ->
                dep.name == it.artifactId.text()
            }
        }.each {
            it.scope*.value = PROVIDED
        }
    }

    private void applyUserPom(Project project, Node pomXml) {
        PomExtension pomExt = project.convention.plugins.pom
        if (pomExt.config) {
            mergePom(pomXml, pomExt.config)
        }
        // apply defaults if required
        if (!pomXml.name) {
            pomXml.appendNode('name', project.name)
        }
        if (project.description && !pomXml.description) {
            pomXml.appendNode('description', project.description)
        }
    }

    /**
     * Complex merge logic is required to avoid tag duplicates.
     * For example, if scm.url tag specified manually  and in pom closure then
     * after using simply '+' to append closure scm section will be duplicated.
     * Correct behaviour is to override existing value and reuse section for other sub nodes.
     *
     * @param pomXml pom xml
     * @param userPom user pom closure
     */
    private void mergePom(Node pomXml, Closure userPom) {
        buildChildrenFromClosure(userPom).each { key, value ->
            insertIntoPom(pomXml, key, value)
        }
    }

    private Map<String, String> buildChildrenFromClosure(Closure c) {
        NodeBuilder b = new NodeBuilder()
        Node newNode = (Node) b.invokeMethod('dummyNode', c)
        flattenNodes(newNode.children())
    }

    @SuppressWarnings('Instanceof')
    private Map<String, String> flattenNodes(List<Node> nodes) {
        Map<String, String> res = [:]
        for (Node node : nodes) {
            if (node.children().isEmpty() || (node.children().size() == 1 && node.children()[0] instanceof String)) {
                res[node.name()] = node.text()
            } else {
                flattenNodes(node.children()).each { key, value ->
                    res["${node.name()}.$key"] = value
                }
            }
        }
        return res
    }

    private void insertIntoPom(Node pomXml, String name, String value) {
        String[] nodes = name.split('\\.')
        Node node = pomXml
        nodes.each {
            node = node[it] ? node[it][0] : node.appendNode(it)
        }
        node.value = value
    }
}
